package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.androidtv.ui.playback.HdrControlsDim
import org.jellyfin.androidtv.ui.player.base.PlayerOverlayLayout
import org.jellyfin.androidtv.ui.player.base.rememberPlayerOverlayVisibility
import org.jellyfin.androidtv.ui.player.base.toast.MediaToastRegistry
import org.jellyfin.androidtv.ui.player.base.toast.MediaToasts
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.mediastream.mediaStreamFlow
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.baseItemFlow
import org.koin.compose.koinInject

// Ephyra: the legacy player's margins (ephyra_side_margin, ephyra_safe_y, ephyra_osd_padding_bottom); the bottom one is 2dp more
// because these buttons are 44dp against the legacy 48dp, so their centres stay where the legacy ones were
private val VideoOverlayPadding = PaddingValues(start = 32.dp, top = 27.dp, end = 32.dp, bottom = 7.dp)

@Composable
fun VideoPlayerOverlay(
	modifier: Modifier = Modifier,
	playbackManager: PlaybackManager = koinInject(),
	mediaToastRegistry: MediaToastRegistry,
) {
	val visibilityState = rememberPlayerOverlayVisibility()
	var showPlaybackInfo by remember { mutableStateOf(false) }

	val entry by rememberQueueEntry(playbackManager)
	val item = entry?.run { baseItemFlow.collectAsState(baseItem) }?.value
	val stream = entry?.run { mediaStreamFlow.collectAsState(mediaStream) }?.value
	// Ephyra: white text and icons glare on an HDR display, so the controls are dimmed then (HdrControlsDim)
	val controlsAlpha = if (isHdrStream(stream)) HdrControlsDim.HDR_ALPHA else 1f

	Box(modifier = modifier) {
		PlayerOverlayLayout(
			visibilityState = visibilityState,
			contentPadding = VideoOverlayPadding,
			header = {
				Column(modifier = Modifier.alpha(controlsAlpha)) {
					VideoPlayerHeader(
						item = item,
						stream = stream,
					)
				}
			},
			controls = {
				VideoPlayerControls(
					playbackManager = playbackManager,
					onPlaybackInfoClick = { showPlaybackInfo = !showPlaybackInfo },
					modifier = Modifier.alpha(controlsAlpha),
				)
			},
		)

		// Playback info overlay - positioned below header area, always visible when enabled
		if (showPlaybackInfo) {
			PlaybackInfoOverlay(
				playbackManager = playbackManager,
				modifier = Modifier
					.align(Alignment.TopStart)
					.padding(start = 48.dp, top = 100.dp)
			)
		}

		MediaToasts(mediaToastRegistry)
	}
}
