package org.jellyfin.playback.jellyfin.playsession

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.RepeatMode
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.SUBTITLES_OFF
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackOrder
import org.jellyfin.sdk.model.api.PlaybackProgressInfo
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.api.QueueItem
import org.jellyfin.sdk.model.extensions.inWholeTicks
import timber.log.Timber
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import org.jellyfin.sdk.model.api.RepeatMode as SdkRepeatMode

private const val QUEUE_PEEK_COUNT = 15

// Velella: the legacy player's reporting rhythm (progress every 3 s while playing, every 15 s while paused to keep the session)
private val SAMPLE_INTERVAL = 1.seconds
private val PROGRESS_INTERVAL_PLAYING = 3.seconds
private val PROGRESS_INTERVAL_PAUSED = 15.seconds

/** Velella: what a report is about: the queue entry, its stream (the play session) and its item. */
private data class PlaySession(val entry: QueueEntry, val stream: PlayableMediaStream, val item: BaseItemDto)

class PlaySessionService(
	private val api: ApiClient,
) : PlayerService() {
	// Velella: every start gets its stop, also when the queue is cleared or moves on (see PlaySessionTracker)
	private val tracker = PlaySessionTracker<PlaySession>()
	private val mutex = Mutex()
	private var lastReport = TimeSource.Monotonic.markNow()

	override suspend fun onInitialize() {
		state.playState.onEach { sync() }.launchIn(coroutineScope)
		manager.queue.entry.onEach { sync() }.launchIn(coroutineScope)

		// Velella: periodic progress, and a start as soon as the next entry's stream is known (no play state change then)
		coroutineScope.launch {
			while (isActive) {
				delay(SAMPLE_INTERVAL)
				val interval = if (state.playState.value == PlayState.PAUSED) PROGRESS_INTERVAL_PAUSED else PROGRESS_INTERVAL_PLAYING
				if (lastReport.elapsedNow() >= interval || currentSession() != tracker.active) sync() else sample()
			}
		}
	}

	private val MediaConversionMethod.playMethod
		get() = when (this) {
			MediaConversionMethod.None -> PlayMethod.DIRECT_PLAY
			MediaConversionMethod.Remux -> PlayMethod.DIRECT_STREAM
			MediaConversionMethod.Transcode -> PlayMethod.TRANSCODE
		}

	private val RepeatMode.remoteRepeatMode
		get() = when (this) {
			RepeatMode.NONE -> SdkRepeatMode.REPEAT_NONE
			RepeatMode.REPEAT_ENTRY_ONCE -> SdkRepeatMode.REPEAT_ONE
			RepeatMode.REPEAT_ENTRY_INFINITE -> SdkRepeatMode.REPEAT_ALL
		}

	private val remotePlaybackOrder
		get() = when (state.playbackOrder.value) {
			org.jellyfin.playback.core.model.PlaybackOrder.DEFAULT -> PlaybackOrder.DEFAULT
			org.jellyfin.playback.core.model.PlaybackOrder.RANDOM -> PlaybackOrder.SHUFFLE
			org.jellyfin.playback.core.model.PlaybackOrder.SHUFFLE -> PlaybackOrder.SHUFFLE
		}

	suspend fun sendUpdateIfActive() {
		coroutineScope.launch { sync() }
	}

	private fun currentSession(): PlaySession? {
		val entry = manager.queue.entry.value ?: return null
		val stream = entry.mediaStream ?: return null
		val item = entry.baseItem ?: return null
		return PlaySession(entry, stream, item)
	}

	private suspend fun sample() = mutex.withLock {
		val current = currentSession() ?: return@withLock
		tracker.observe(current, withContext(Dispatchers.Main) { state.positionInfo.active })
	}

	private suspend fun sync() = mutex.withLock {
		val current = currentSession()
		val position = if (current == null) null else withContext(Dispatchers.Main) { state.positionInfo.active }

		for (report in tracker.update(current, state.playState.value, position)) {
			when (report) {
				is PlaySessionReport.Start -> sendStreamStart(report.session, position ?: Duration.ZERO)
				is PlaySessionReport.Progress -> sendStreamUpdate(report.session, position ?: Duration.ZERO)
				is PlaySessionReport.Stop -> sendStreamStop(report.session, report.position, report.failed)
			}
		}
		lastReport = TimeSource.Monotonic.markNow()
	}

	private suspend fun getQueue(): List<QueueItem> {
		// The queues are lazy loaded so we only load a small amount of items to set as queue on the
		// backend.
		return manager.queue
			.peekNext(QUEUE_PEEK_COUNT)
			.mapNotNull { it.baseItem }
			.map { QueueItem(id = it.id, playlistItemId = it.playlistItemId) }
	}

	private suspend fun sendStreamStart(session: PlaySession, position: Duration) {
		runCatching {
			api.sessionApi.reportPlaybackStart(
				PlaybackStartInfo(
					itemId = session.item.id,
					playSessionId = session.stream.identifier,
					mediaSourceId = session.stream.mediaSourceId,
					// Velella: the server closes a live stream it opened (tuner, shared M3U stream) when the stop report names it
					liveStreamId = session.stream.liveStreamId,
					playlistItemId = session.item.playlistItemId,
					canSeek = true,
					isMuted = state.volume.muted,
					volumeLevel = (state.volume.volume * 100).roundToInt(),
					isPaused = state.playState.value != PlayState.PLAYING,
					aspectRatio = state.videoSize.value.aspectRatio.toString(),
					positionTicks = position.inWholeTicks,
					audioStreamIndex = state.selectedTracks.value.audioIndex,
					subtitleStreamIndex = state.selectedTracks.value.subtitleIndex ?: SUBTITLES_OFF,
					playMethod = session.stream.conversionMethod.playMethod,
					repeatMode = state.repeatMode.value.remoteRepeatMode,
					nowPlayingQueue = getQueue(),
					playbackOrder = remotePlaybackOrder,
				)
			)
		}.onFailure { error -> Timber.w(error, "Failed to send playback start event") }
	}

	private suspend fun sendStreamUpdate(session: PlaySession, position: Duration) {
		runCatching {
			api.sessionApi.reportPlaybackProgress(
				PlaybackProgressInfo(
					itemId = session.item.id,
					playSessionId = session.stream.identifier,
					mediaSourceId = session.stream.mediaSourceId,
					// Velella: the server closes a live stream it opened (tuner, shared M3U stream) when the stop report names it
					liveStreamId = session.stream.liveStreamId,
					playlistItemId = session.item.playlistItemId,
					canSeek = true,
					isMuted = state.volume.muted,
					volumeLevel = (state.volume.volume * 100).roundToInt(),
					isPaused = state.playState.value != PlayState.PLAYING,
					aspectRatio = state.videoSize.value.aspectRatio.toString(),
					positionTicks = position.inWholeTicks,
					audioStreamIndex = state.selectedTracks.value.audioIndex,
					subtitleStreamIndex = state.selectedTracks.value.subtitleIndex ?: SUBTITLES_OFF,
					playMethod = session.stream.conversionMethod.playMethod,
					repeatMode = state.repeatMode.value.remoteRepeatMode,
					nowPlayingQueue = getQueue(),
					playbackOrder = remotePlaybackOrder,
				)
			)
		}.onFailure { error -> Timber.w(error, "Failed to send playback update event") }
	}

	private suspend fun sendStreamStop(session: PlaySession, position: Duration, failed: Boolean) {
		runCatching {
			api.sessionApi.reportPlaybackStopped(
				PlaybackStopInfo(
					itemId = session.item.id,
					playSessionId = session.stream.identifier,
					mediaSourceId = session.stream.mediaSourceId,
					// Velella: the server closes a live stream it opened (tuner, shared M3U stream) when the stop report names it
					liveStreamId = session.stream.liveStreamId,
					playlistItemId = session.item.playlistItemId,
					positionTicks = position.inWholeTicks,
					failed = failed,
					nowPlayingQueue = getQueue(),
				)
			)
		}.onFailure { error -> Timber.w(error, "Failed to send playback stop event") }
	}
}
