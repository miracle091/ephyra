package org.jellyfin.playback.media3.exoplayer

import android.app.ActivityManager
import android.content.Context
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.SubtitleView
import io.github.peerless2012.ass.media.AssHandler
import io.github.peerless2012.ass.media.factory.AssRenderersFactory
import io.github.peerless2012.ass.media.kt.withAssMkvSupport
import io.github.peerless2012.ass.media.parser.AssSubtitleParserFactory
import io.github.peerless2012.ass.media.type.AssRenderType
import io.github.peerless2012.ass.media.widget.AssSubtitleView
import org.jellyfin.playback.core.backend.BasePlayerBackend
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.mediastream.MediaStreamSubtitleTrack
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.mediastream.mediatype.MediaType
import org.jellyfin.playback.core.mediastream.mediatype.mediaType
import org.jellyfin.playback.core.mediastream.normalizationGain
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.model.SelectedTracks
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.startPosition
import org.jellyfin.playback.core.support.PlaySupportReport
import org.jellyfin.playback.core.timedevent.TimedEvent
import org.jellyfin.playback.core.ui.PlayerSubtitleView
import org.jellyfin.playback.core.ui.PlayerSurfaceView
import org.jellyfin.playback.media3.exoplayer.mapping.SubtitleSelection
import org.jellyfin.playback.media3.exoplayer.mapping.externalSubtitleId
import org.jellyfin.playback.media3.exoplayer.mapping.externalSubtitleIndex
import org.jellyfin.playback.media3.exoplayer.mapping.findAudioGroupPosition
import org.jellyfin.playback.media3.exoplayer.mapping.findSubtitleSelection
import org.jellyfin.playback.media3.exoplayer.mapping.subtitleMimeType
import org.jellyfin.playback.media3.exoplayer.mapping.toPlaybackError
import org.jellyfin.playback.media3.exoplayer.shield.BitstreamFirstRenderersFactory
import org.jellyfin.playback.media3.exoplayer.shield.ShieldLoadControl
import org.jellyfin.playback.media3.exoplayer.shield.ShieldMediaCodecSelector
import org.jellyfin.playback.media3.exoplayer.support.getPlaySupportReport
import org.jellyfin.playback.media3.exoplayer.support.toFormats
import timber.log.Timber
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
class ExoPlayerBackend(
	private val context: Context,
	// Ephyra: read each time a player is built, so a changed setting applies from the next playback
	private val exoPlayerOptions: () -> ExoPlayerOptions,
) : BasePlayerBackend() {
	companion object {
		const val TS_SEARCH_BYTES_LM = TsExtractor.TS_PACKET_SIZE * 1800
		const val TS_SEARCH_BYTES_HM = TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES
		const val MEDIA_ITEM_COUNT_MAX = 10
	}

	private class PlayerInstance(val player: ExoPlayer, val assHandler: AssHandler?, val options: ExoPlayerOptions)

	private var currentStream: PlayableMediaStream? = null
	private var subtitleView: SubtitleView? = null
	private val audioPipeline = ExoPlayerAudioPipeline()
	private val audioNightMode = AudioNightMode()
	private var audioSessionId = C.AUDIO_SESSION_ID_UNSET
	private var audioAttributeState = AudioAttributeState()
	private var timedEventState = TimedEventState()
	private var lastKnownDuration: Duration? = null
	private var audioSelectionDone: PlayableMediaStream? = null
	private var subtitleSelectionDone: PlayableMediaStream? = null
	private var selectedTracks = SelectedTracks.NONE

	// Ephyra: the player lives from first use to stop(), which releases it (decoders, buffers, libass); the views outlive it
	private var instance: PlayerInstance? = null
	private var surfaceView: PlayerSurfaceView? = null
	private var subtitleContainer: PlayerSubtitleView? = null
	private var speed = 1f

	private val exoPlayer: ExoPlayer
		get() = instance?.player ?: buildInstance().player

	private fun buildInstance(): PlayerInstance {
		val options = exoPlayerOptions()
		val assHandler = if (options.enableLibass) AssHandler(AssRenderType.OVERLAY_OPEN_GL) else null
		val instance = PlayerInstance(createPlayer(options, assHandler), assHandler, options)
		this.instance = instance
		instance.player.setVideoSurfaceView(surfaceView?.surface)
		instance.player.setPlaybackSpeed(speed)
		subtitleContainer?.let(::attachSubtitleView)
		return instance
	}

	private fun createPlayer(exoPlayerOptions: ExoPlayerOptions, assHandler: AssHandler?): ExoPlayer {
		val dataSourceFactory = DefaultDataSource.Factory(
			context,
			exoPlayerOptions.baseDataSourceFactory,
		)
		val extractorsFactory = DefaultExtractorsFactory().apply {
			val isLowRamDevice = context.getSystemService<ActivityManager>()?.isLowRamDevice == true
			setTsExtractorTimestampSearchBytes(
				when (isLowRamDevice) {
					true -> TS_SEARCH_BYTES_LM
					false -> TS_SEARCH_BYTES_HM
				}
			)
			setConstantBitrateSeekingEnabled(true)
			setConstantBitrateSeekingAlwaysEnabled(true)
		}

		val mediaSourceFactory = if (assHandler != null) {
			val assSubtitleParserFactory = AssSubtitleParserFactory(assHandler)
			val assExtractorsFactory = extractorsFactory.withAssMkvSupport(assSubtitleParserFactory, assHandler)
			DefaultMediaSourceFactory(dataSourceFactory, assExtractorsFactory).apply {
				setSubtitleParserFactory(assSubtitleParserFactory)
			}
		} else DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

		// Ephyra: the default audio sink plus a tee that measures the loudness for the music player's backdrop (AudioLevel)
		// Ephyra: when FFmpeg is preferred it still steps aside for formats the sink can pass through as bitstream
		val renderersFactory = object : BitstreamFirstRenderersFactory(context, exoPlayerOptions.preferFfmpeg) {
			override fun buildAudioSink(
				context: Context,
				enableFloatOutput: Boolean,
				enableAudioOutputPlaybackParameters: Boolean,
			): AudioSink = DefaultAudioSink.Builder(context)
				.setEnableFloatOutput(enableFloatOutput)
				.setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParameters)
				.setAudioProcessors(arrayOf(TeeAudioProcessor(AudioLevelSink())))
				.build()
		}.apply {
			setMediaCodecSelector(ShieldMediaCodecSelector())
			setEnableDecoderFallback(true)
			setExtensionRendererMode(
				when (exoPlayerOptions.preferFfmpeg) {
					true -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
					false -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
				}
			)
		}.let { renderersFactory ->
			if (assHandler != null) AssRenderersFactory(assHandler, renderersFactory)
			else renderersFactory
		}

		val loadControl = ShieldLoadControl.build(
			context,
			exoPlayerOptions.minBufferDuration?.inWholeMilliseconds?.toInt() ?: DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
			exoPlayerOptions.maxBufferDuration?.inWholeMilliseconds?.toInt() ?: DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
			exoPlayerOptions.bufferForPlaybackDuration?.inWholeMilliseconds?.toInt() ?: DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
			exoPlayerOptions.bufferForPlaybackAfterRebufferDuration?.inWholeMilliseconds?.toInt()
				?: DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
		)

		return ExoPlayer.Builder(context)
			.setLoadControl(loadControl)
			.setRenderersFactory(renderersFactory)
			.setTrackSelector(DefaultTrackSelector(context).apply {
				setParameters(buildUponParameters().apply {
					setAudioOffloadPreferences(
						TrackSelectionParameters.AudioOffloadPreferences.DEFAULT.buildUpon().apply {
							// Ephyra: a mains-powered TV box gains nothing from DSP offload, and offload is preferred over HDMI bitstream
							// passthrough when the platform offers both (DefaultAudioSink.getFormatSupport), so keep it off
							setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
						}.build()
					)
					setAllowInvalidateSelectionsOnRendererCapabilitiesChange(true)
				})
			})
			.setMediaSourceFactory(mediaSourceFactory)
			.setPauseAtEndOfMediaItems(true)
			.build()
			.also { player ->
				player.addListener(PlayerListener(player))
				player.videoChangeFrameRateStrategy = exoPlayerOptions.videoChangeFrameRateStrategy

				if (exoPlayerOptions.enableDebugLogging) {
					player.addAnalyticsListener(EventLogger())
				}

				assHandler?.init(player)
			}
	}

	// Ephyra: bound to its own player, so a late callback never builds a new one
	inner class PlayerListener(private val exoPlayer: ExoPlayer) : Player.Listener {
		override fun onIsPlayingChanged(isPlaying: Boolean) {
			val state = when {
				isPlaying -> PlayState.PLAYING
				exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playbackState == Player.STATE_ENDED -> PlayState.STOPPED
				else -> PlayState.PAUSED
			}
			listener?.onPlayStateChange(state)
		}

		override fun onPlayerError(error: PlaybackException) {
			// Ephyra: say why, so the retry logic and the UI can act on it
			Timber.e(error, "Playback failed (${error.errorCodeName})")
			listener?.onPlayError(error.toPlaybackError())
			listener?.onPlayStateChange(PlayState.ERROR)
		}

		override fun onVideoSizeChanged(size: VideoSize) {
			if (size != VideoSize.UNKNOWN) {
				// Ephyra: the displayed width, so anamorphic video (DVD, some MKV) keeps its shape instead of the stored pixel grid's
				val displayWidth = (size.width * size.pixelWidthHeightRatio).roundToInt()
				listener?.onVideoSizeChange(displayWidth, size.height)
			}
		}

		override fun onCues(cueGroup: CueGroup) {
			subtitleView?.setCues(cueGroup.cues)
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			onIsPlayingChanged(exoPlayer.isPlaying)
		}

		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
			if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
				listener?.onMediaStreamEnd(requireNotNull(currentStream))
			}
		}

		override fun onAudioSessionIdChanged(audioSessionId: Int) {
			audioPipeline.setAudioSessionId(audioSessionId)
			// Ephyra: the session comes once, often before the first item is known (the player is built early): decide in playItem too
			this@ExoPlayerBackend.audioSessionId = audioSessionId
			updateNightMode()
		}

		override fun onTracksChanged(tracks: Tracks) {
			selectServerAudioTrack(tracks)
			selectServerSubtitle(tracks)
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			val queueEntry = mediaItem?.localConfiguration?.tag as? QueueEntry
			audioPipeline.normalizationGain = queueEntry?.normalizationGain
		}

		override fun onTimelineChanged(timeline: Timeline, reason: Int) {
			val duration = exoPlayer.duration.takeUnless { it == C.TIME_UNSET }?.milliseconds
			if (duration == lastKnownDuration) return
			timedEventState.onDurationChange(exoPlayer, duration)
			lastKnownDuration = duration
		}

		override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
			timedEventState.onSeek(oldPosition.positionMs.milliseconds, newPosition.positionMs.milliseconds, lastKnownDuration ?: Duration.ZERO)
		}
	}

	/**
	 * Ephyra: on direct play ExoPlayer would pick the audio track by its own rules (default flag, device language), so select the
	 * one the server chose (the requested track or the default from the user's language preferences). Done once per stream, as soon
	 * as the audio tracks are known, so a track picked later in the player is not overridden. A remuxed or transcoded stream holds
	 * only the requested track: nothing to select.
	 */
	private fun selectServerAudioTrack(tracks: Tracks) {
		val stream = currentStream ?: return
		// The media id check: tracks of another (prepared) item must not be matched against this stream
		if (
			stream.conversionMethod != MediaConversionMethod.None ||
			audioSelectionDone == stream ||
			exoPlayer.currentMediaItem?.mediaId != stream.hashCode().toString()
		) return

		val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
		if (audioGroups.isEmpty()) return
		audioSelectionDone = stream

		val wantedIndex = stream.defaultAudioStreamIndex ?: return
		val position = findSupportedAudioGroupPosition(stream, wantedIndex, audioGroups) ?: return
		val group = audioGroups[position]
		if (group.isSelected) return

		Timber.i("Selecting audio track $position for server audio stream $wantedIndex")
		exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
			.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
			.build()
	}

	/**
	 * Ephyra: show the subtitle the server chose from the user's settings (or none), instead of ExoPlayer's own rules. Done once per
	 * stream, as soon as its tracks are known, so a subtitle picked later in the player is not overridden.
	 */
	private fun selectServerSubtitle(tracks: Tracks) {
		val stream = currentStream ?: return
		if (
			subtitleSelectionDone == stream ||
			tracks.groups.isEmpty() ||
			exoPlayer.currentMediaItem?.mediaId != stream.hashCode().toString()
		) return
		subtitleSelectionDone = stream

		val selection = applySubtitle(exoPlayer, stream, tracks, stream.defaultSubtitleStreamIndex)
		Timber.i("Subtitle for server subtitle stream ${stream.defaultSubtitleStreamIndex}: $selection")
	}

	/**
	 * Ephyra: maps the subtitle [index] (or none) to the player's text tracks and selects it. When it can't be mapped, the
	 * player's own choice is restored, or left untouched with [untouchedIfUnmapped] (a pick that a new stream will bring).
	 */
	private fun applySubtitle(
		exoPlayer: ExoPlayer,
		stream: PlayableMediaStream,
		tracks: Tracks,
		index: Int?,
		untouchedIfUnmapped: Boolean = false,
	): SubtitleSelection {
		val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
		val selection = findSubtitleSelection(
			tracks = stream.tracks,
			wantedIndex = index,
			groupExternalIndexes = textGroups.map { externalSubtitleIndex(it.mediaTrackGroup.getFormat(0).id) },
			directPlay = stream.conversionMethod == MediaConversionMethod.None,
		)
		if (selection == SubtitleSelection.Keep && untouchedIfUnmapped) return selection

		val parameters = exoPlayer.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT)
		when (selection) {
			SubtitleSelection.Off -> parameters.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
			is SubtitleSelection.Group -> parameters
				.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
				.setOverrideForType(TrackSelectionOverride(textGroups[selection.position].mediaTrackGroup, 0))

			SubtitleSelection.Keep -> parameters.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
		}
		exoPlayer.trackSelectionParameters = parameters.build()
		return selection
	}

	override fun selectAudioTrack(index: Int): Boolean {
		val exoPlayer = instance?.player ?: return false
		val stream = currentStream ?: return false
		// A converted stream holds only the requested audio track
		if (stream.conversionMethod != MediaConversionMethod.None) return false

		val audioGroups = exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
		val position = findSupportedAudioGroupPosition(stream, index, audioGroups) ?: return false
		Timber.i("Selecting audio track $position for audio stream $index")
		exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
			.setOverrideForType(TrackSelectionOverride(audioGroups[position].mediaTrackGroup, 0))
			.build()
		audioSelectionDone = stream
		updateSelectedTracks(selectedTracks.copy(audioIndex = index))
		return true
	}

	override fun selectSubtitleTrack(index: Int?): Boolean {
		val exoPlayer = instance?.player ?: return false
		val stream = currentStream ?: return false
		// On a converted stream the server put its subtitle in the stream (manifest or burnt in): only files loaded beside it are
		// the player's to switch
		fun isSideLoaded(subtitleIndex: Int?) = subtitleIndex == null || stream.tracks
			.any { it is MediaStreamSubtitleTrack && it.index == subtitleIndex && it.externalUrl != null }
		val converted = stream.conversionMethod != MediaConversionMethod.None
		if (converted && !(isSideLoaded(selectedTracks.subtitleIndex) && isSideLoaded(index))) return false

		val selection = applySubtitle(exoPlayer, stream, exoPlayer.currentTracks, index, untouchedIfUnmapped = true)
		Timber.i("Subtitle for subtitle stream $index: $selection")
		if (selection == SubtitleSelection.Keep) return false
		subtitleSelectionDone = stream
		updateSelectedTracks(selectedTracks.copy(subtitleIndex = index))
		return true
	}

	/** Ephyra: night mode on the player's audio session while a video plays, as in the legacy player; music keeps its dynamics. */
	private fun updateNightMode() {
		val enabled = instance?.options?.audioNightMode == true
		val video = currentStream?.queueEntry?.mediaType == MediaType.Video
		if (enabled && video) audioNightMode.apply(audioSessionId) else audioNightMode.release()
	}

	private fun updateSelectedTracks(tracks: SelectedTracks) {
		if (tracks == selectedTracks) return
		selectedTracks = tracks
		listener?.onSelectedTracksChange(tracks)
	}

	override fun supportsStream(
		stream: MediaStream
	): PlaySupportReport = exoPlayer.getPlaySupportReport(stream.toFormats())

	override fun setSurfaceView(surfaceView: PlayerSurfaceView?) {
		this.surfaceView = surfaceView
		instance?.player?.setVideoSurfaceView(surfaceView?.surface)
	}

	override fun setSubtitleView(surfaceView: PlayerSubtitleView?) {
		detachSubtitleView()
		subtitleContainer = surfaceView
		if (surfaceView != null) attachSubtitleView(surfaceView)
	}

	/** Ephyra: a subtitle view for the current player (its libass handler), added once a player exists. */
	private fun attachSubtitleView(container: PlayerSubtitleView) {
		val current = instance ?: return
		val view = SubtitleView(container.context).apply {
			current.options.subtitleStyle?.let(::applyStyle)
			current.assHandler?.let { addView(AssSubtitleView(container.context, it)) }
		}
		subtitleView = view
		container.addView(view)
	}

	private fun detachSubtitleView() {
		(subtitleView?.parent as? ViewGroup)?.removeView(subtitleView)
		subtitleView = null
	}

	override fun prepareItem(item: QueueEntry) {
		val stream = requireNotNull(item.mediaStream)
		val mediaItem = MediaItem.Builder().apply {
			setTag(item)
			setMediaId(stream.hashCode().toString())
			setUri(stream.url)
			// Ephyra: subtitles the server hands out as separate files, loaded beside the stream as in the legacy player
			setSubtitleConfigurations(stream.tracks.filterIsInstance<MediaStreamSubtitleTrack>().mapNotNull { it.toSubtitleConfiguration() })
		}.build()

		// Remove any excessive items from the start
		while (exoPlayer.mediaItemCount > MEDIA_ITEM_COUNT_MAX - 1) exoPlayer.removeMediaItem(0)

		// Add new item to the end of the media item list
		exoPlayer.addMediaItem(mediaItem)

		// Instruct exoplayer to prepare
		exoPlayer.prepare()
	}

	override fun playItem(item: QueueEntry) {
		val stream = requireNotNull(item.mediaStream)
		if (currentStream == stream) return

		val previousStream = currentStream
		currentStream = stream
		// Ephyra: the server's choice until the player shows otherwise
		updateSelectedTracks(SelectedTracks(stream.defaultAudioStreamIndex, stream.defaultSubtitleStreamIndex))
		updateNightMode()

		var preparedItemIndex = (0 until exoPlayer.mediaItemCount).firstOrNull { index ->
			exoPlayer.getMediaItemAt(index).mediaId == stream.hashCode().toString()
		}

		// Prepare the item now if it doesn't exist yet
		if (preparedItemIndex == null) {
			prepareItem(item)
			preparedItemIndex = exoPlayer.mediaItemCount - 1
		}

		// Seek to prepared media item; Ephyra: at the entry's start position (resume) when it has one, applied once
		val startPosition = item.startPosition
		item.startPosition = null
		when {
			startPosition != null -> exoPlayer.seekTo(preparedItemIndex, startPosition.inWholeMilliseconds)
			preparedItemIndex == exoPlayer.currentMediaItemIndex - 1 -> exoPlayer.seekToPreviousMediaItem()
			preparedItemIndex == exoPlayer.currentMediaItemIndex + 1 -> exoPlayer.seekToNextMediaItem()
			preparedItemIndex == exoPlayer.currentMediaItemIndex -> Unit
			else -> exoPlayer.seekTo(preparedItemIndex, 0)
		}

		// Ephyra: a new stream of the same entry (another track) plays as another media item: move the entry's timed events to it
		if (previousStream?.queueEntry === item) timedEventState.recreate(exoPlayer, lastKnownDuration)

		// Update audio attributes
		val contentType = when (item.mediaType) {
			MediaType.Video -> C.AUDIO_CONTENT_TYPE_MOVIE
			MediaType.Audio -> C.AUDIO_CONTENT_TYPE_MUSIC
			MediaType.Unknown -> C.AUDIO_CONTENT_TYPE_UNKNOWN
		}

		audioAttributeState.updateAudioAttributes(
			builder = {
				setContentType(contentType)
				setUsage(C.USAGE_MEDIA)
			},
			onChange = { audioAttributes ->
				exoPlayer.setAudioAttributes(audioAttributes, true)
			}
		)

		// Enjoy!
		Timber.i("Playing ${item.mediaStream?.url?.redactApiKey()}")
		exoPlayer.play()
	}

	override fun play() {
		// If the item has ended, revert first so the item will start over again
		if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0)
		exoPlayer.play()
	}

	override fun pause() {
		instance?.player?.pause()
	}

	override fun stop() {
		// Ephyra: stop first so the listeners hear STOPPED, then release; the next item builds a player with the current options
		instance?.let { current ->
			current.player.stop()
			detachSubtitleView()
			current.player.release()
		}
		instance = null
		audioNightMode.release()
		audioSessionId = C.AUDIO_SESSION_ID_UNSET
		currentStream = null
		audioSelectionDone = null
		subtitleSelectionDone = null
		updateSelectedTracks(SelectedTracks.NONE)
		lastKnownDuration = null
		audioAttributeState = AudioAttributeState()
		timedEventState = TimedEventState()
	}

	override fun seekTo(position: Duration) {
		val exoPlayer = instance?.player ?: return
		if (!exoPlayer.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) || !exoPlayer.isCurrentMediaItemSeekable) {
			Timber.w("Trying to seek but ExoPlayer doesn't support it for the current item")
		}

		exoPlayer.seekTo(position.inWholeMilliseconds)
	}

	override fun setScrubbing(scrubbing: Boolean) {
		instance?.player?.isScrubbingModeEnabled = scrubbing
	}

	override fun setSpeed(speed: Float) {
		// Ephyra: kept for the next player too, as PlayerState keeps it; without a player there is nothing to change yet
		this.speed = speed
		val exoPlayer = instance?.player ?: return
		if (!exoPlayer.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) {
			Timber.w("Trying to change speed but ExoPlayer doesn't support it for the current item")
		}

		exoPlayer.setPlaybackSpeed(speed)
	}

	// Ephyra: polled often (UI, play session); without a player there is nothing to report, so don't build one
	override fun getPositionInfo(): PositionInfo {
		val exoPlayer = instance?.player ?: return PositionInfo.EMPTY
		return PositionInfo(
			active = exoPlayer.currentPosition.milliseconds,
			buffer = exoPlayer.bufferedPosition.milliseconds,
			duration = lastKnownDuration ?: Duration.ZERO,
		)
	}

	override fun setTimedEvents(timedEvents: List<TimedEvent>) {
		timedEventState.setTimedEvents(exoPlayer, timedEvents)
	}
}

/** Ephyra: a subtitle file handed out by the server, as a side-loaded track of the media item; null for the stream's own tracks. */
@OptIn(UnstableApi::class)
private fun MediaStreamSubtitleTrack.toSubtitleConfiguration(): MediaItem.SubtitleConfiguration? {
	val url = externalUrl ?: return null
	var selectionFlags = 0
	if (isDefault) selectionFlags = selectionFlags or C.SELECTION_FLAG_DEFAULT
	if (isForced) selectionFlags = selectionFlags or C.SELECTION_FLAG_FORCED
	return MediaItem.SubtitleConfiguration.Builder(url.toUri())
		.setId(externalSubtitleId(index))
		.setMimeType(subtitleMimeType(codec, url))
		.setLanguage(language)
		.setLabel(title)
		.setSelectionFlags(selectionFlags)
		.build()
}

/** The position in [audioGroups] of the player track for the server audio stream [wantedIndex], or null (logged) if there is none. */
private fun findSupportedAudioGroupPosition(stream: PlayableMediaStream, wantedIndex: Int, audioGroups: List<Tracks.Group>): Int? {
	val position = findAudioGroupPosition(stream.tracks, wantedIndex, audioGroups.size)
	if (position == null) {
		Timber.w("Cannot map server audio stream $wantedIndex to a player track (${audioGroups.size} player audio tracks)")
		return null
	}

	if (!audioGroups[position].isSupported) {
		Timber.w("Server audio stream $wantedIndex is not supported by the player, keeping its own choice")
		return null
	}
	return position
}
