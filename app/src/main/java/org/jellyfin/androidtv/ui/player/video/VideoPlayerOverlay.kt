package org.jellyfin.androidtv.ui.player.video

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.VelellaTheme
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.androidtv.ui.playback.HdrControlsDim
import org.jellyfin.androidtv.ui.playback.overlay.SkipButtonState
import org.jellyfin.androidtv.ui.playback.overlay.SkipOverlayComposable
import org.jellyfin.androidtv.ui.player.base.PlayerOverlayLayout
import org.jellyfin.androidtv.ui.player.base.rememberPlayerOverlayVisibility
import org.jellyfin.androidtv.ui.player.base.toast.MediaToastRegistry
import org.jellyfin.androidtv.ui.player.base.toast.MediaToasts
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.mediastream.mediaStreamFlow
import org.jellyfin.playback.core.queue.startPosition
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.baseItemFlow
import org.koin.compose.koinInject
import kotlin.time.Duration

/**
 * Velella: the playback keys' [action]; the play/pause key and centre play, go on from a pause, or pause while playing. [onPause] runs
 * after a pause.
 */
private fun PlaybackManager.perform(action: VideoPlayerKeyAction, mediaToastRegistry: MediaToastRegistry, onPause: () -> Unit) {
	val command = when (action) {
		VideoPlayerKeyAction.PLAY -> PlayPauseCommand.UNPAUSE
		VideoPlayerKeyAction.PAUSE -> PlayPauseCommand.PAUSE
		VideoPlayerKeyAction.PLAY_PAUSE -> playPauseCommand(state.playState.value)
		VideoPlayerKeyAction.FAST_FORWARD -> null.also { state.fastForward() }
		VideoPlayerKeyAction.REWIND -> null.also { state.rewind() }
		// Handled by the overlay
		VideoPlayerKeyAction.STOP, VideoPlayerKeyAction.SUBTITLES_MENU, VideoPlayerKeyAction.AUDIO_MENU,
		VideoPlayerKeyAction.QUICK_SEEK_FORWARD, VideoPlayerKeyAction.QUICK_SEEK_REWIND -> null
	}
	command?.let { perform(it, mediaToastRegistry) }
	if (command == PlayPauseCommand.PAUSE) onPause()
}

// Velella: the legacy player's margins (velella_side_margin, velella_safe_y, velella_osd_padding_bottom); the bottom one is 2dp more
// because these buttons are 44dp against the legacy 48dp, so their centres stay where the legacy ones were
private val VideoOverlayPadding = PaddingValues(start = 32.dp, top = 27.dp, end = 32.dp, bottom = 7.dp)

@Composable
fun VideoPlayerOverlay(
	modifier: Modifier = Modifier,
	playbackManager: PlaybackManager = koinInject(),
	mediaToastRegistry: MediaToastRegistry,
	liveTvSwitch: LiveTvSwitch? = null,
) {
	var showPlaybackInfo by remember { mutableStateOf(false) }

	val entry by rememberQueueEntry(playbackManager)
	val item = entry?.run { baseItemFlow.collectAsState(baseItem) }?.value
	val stream = entry?.run { mediaStreamFlow.collectAsState(mediaStream) }?.value
	// Velella: closing the live channel bar or a new channel gives the overlay the focus back
	val channelBar = remember { ChannelBarState() }
	val visibilityState = rememberPlayerOverlayVisibility(focusKey = channelBar.closings to item?.id)
	// Velella: white text and icons glare on an HDR display, so the controls are dimmed then (HdrControlsDim)
	val controlsAlpha = if (isHdrStream(stream)) HdrControlsDim.HDR_ALPHA else 1f
	// Velella: a live channel's programmes and buttons (null for anything else)
	val live = rememberLiveControls(item, stream, visibilityState, channelBar, liveTvSwitch)

	// Velella: the trickplay thumbnails are downloaded as playback starts, so scrubbing shows them at once
	PrefetchTrickplay(item, stream?.mediaSourceId, entry?.startPosition ?: Duration.ZERO)

	// Velella: the legacy player's keys (VideoPlayerKeyAction.kt); Captions and Audio track show the controls with that menu open
	var menuRequest by remember { mutableStateOf<VideoPlayerKeyAction?>(null) }
	val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
	// Velella: the skip button of an "ask to skip" segment takes its keys first, as in the legacy player
	val skipSegment = rememberSkipSegment(playbackManager, item, visibilityState.visible)
	// Velella: left and right with the controls hidden add up to one seek (QuickSeek.kt)
	val quickSeek = rememberQuickSeek(playbackManager, mediaToastRegistry)

	val onKey = onKey@{ keyCode: Int ->
		liveOverlayKey(keyCode, live, channelBar.isOpen, visibilityState.visible)?.let { return@onKey it }

		val skipAction = skipKeyAction(skipSegment.state, keyCode)
		val action = if (skipAction == null) videoPlayerKeyAction(keyCode, visibilityState.visible)?.unlessSeekingLive(live) else null
		when (action) {
			null -> skipAction?.let(skipSegment.onKey)
			VideoPlayerKeyAction.STOP -> backDispatcher?.onBackPressed()
			VideoPlayerKeyAction.SUBTITLES_MENU, VideoPlayerKeyAction.AUDIO_MENU -> {
				menuRequest = action
				visibilityState.show()
			}
			// Velella: a pause by the user shows the controls
			else -> if (!quickSeek.onKey(action, keyCode)) playbackManager.perform(action, mediaToastRegistry, onPause = visibilityState.show)
		}
		skipAction != null || action != null
	}

	Box(
		modifier = modifier.onPreviewKeyEvent { event ->
			event.type == KeyEventType.KeyDown && onKey(event.nativeKeyEvent.keyCode)
		}
	) {
		PlayerOverlayLayout(
			visibilityState = visibilityState,
			contentPadding = VideoOverlayPadding,
			header = {
				Column(modifier = Modifier.alpha(controlsAlpha)) { VideoPlayerHeader(item, stream, live?.info) }
			},
			controls = {
				Box(modifier = Modifier.alpha(controlsAlpha)) {
					VideoPlayerControls(
						playbackManager = playbackManager,
						onPlaybackInfoClick = { showPlaybackInfo = !showPlaybackInfo },
						menuRequest = menuRequest,
						onMenuRequestHandled = { menuRequest = null },
						live = live,
					)
				}
			},
		)

		LiveChannelBarHost(channelBar, live)

		VideoPlayerOverlayExtras(playbackManager, showPlaybackInfo, skipSegment, mediaToastRegistry)
	}
}

/** The playback info panel when asked for, Velella's skip button and the toasts, over the video. */
@Composable
private fun BoxScope.VideoPlayerOverlayExtras(
	playbackManager: PlaybackManager,
	showPlaybackInfo: Boolean,
	skipSegment: SkipSegment,
	mediaToastRegistry: MediaToastRegistry,
) {
	// Playback info overlay - positioned below header area, always visible when enabled
	if (showPlaybackInfo) {
		PlaybackInfoOverlay(
			playbackManager = playbackManager,
			modifier = Modifier
				.align(Alignment.TopStart)
				.padding(start = 48.dp, top = 100.dp)
		)
	}

	// Velella: the legacy player's skip button, in the user's theme colours, at the bottom right
	Box(modifier = Modifier.align(Alignment.BottomEnd)) {
		VelellaTheme {
			SkipOverlayComposable(visible = skipSegment.state == SkipButtonState.SHOWN)
		}
	}

	MediaToasts(mediaToastRegistry)
}
