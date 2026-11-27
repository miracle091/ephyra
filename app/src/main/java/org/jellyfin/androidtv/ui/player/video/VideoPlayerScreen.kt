package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.flow.map
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ZoomMode
import org.jellyfin.androidtv.ui.ScreensaverLock
import org.jellyfin.androidtv.ui.player.base.PlayerSubtitles
import org.jellyfin.androidtv.ui.player.base.PlayerSurface
import org.jellyfin.androidtv.ui.player.base.toast.MediaToastRegistry
import org.jellyfin.androidtv.ui.player.video.toast.rememberPlaybackManagerMediaToastEmitter
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.koin.compose.koinInject

private const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f

@Composable
fun VideoPlayerScreen() {
	val playbackManager = koinInject<PlaybackManager>()

	val backgroundService = koinInject<BackgroundService>()
	LaunchedEffect(backgroundService) {
		backgroundService.clearBackgrounds()
	}

	val playing by remember {
		playbackManager.state.playState.map { it == PlayState.PLAYING }
	}.collectAsState(false)
	ScreensaverLock(
		enabled = playing,
	)

	val videoSize by playbackManager.state.videoSize.collectAsState()
	val aspectRatio = videoSize.aspectRatio.takeIf { !it.isNaN() && it > 0f } ?: DEFAULT_VIDEO_ASPECT_RATIO

	val coroutineScope = rememberCoroutineScope()
	val mediaToastRegistry = remember { MediaToastRegistry(coroutineScope) }
	rememberPlaybackManagerMediaToastEmitter(playbackManager, mediaToastRegistry)

	// Velella: the user's zoom mode for the picture; subtitles stay inside the whole (fitted) picture so none is cut off
	val userPreferences = koinInject<UserPreferences>()
	val zoomMode = remember { userPreferences[UserPreferences.playerZoomMode] }

	BoxWithConstraints(
		modifier = Modifier
			.background(Color.Black)
			.fillMaxSize()
	) {
		val container = IntSize(constraints.maxWidth, constraints.maxHeight)
		val density = LocalDensity.current
		val pictureSize = with(density) { videoFrameSize(container, aspectRatio, zoomMode).toSize().toDpSize() }
		val subtitleSize = with(density) { videoFrameSize(container, aspectRatio, ZoomMode.FIT).toSize().toDpSize() }

		PlayerSurface(
			playbackManager = playbackManager,
			modifier = Modifier
				// Larger than the screen when cropping: centred, the overflow is cut off by the screen's edges
				.requiredSize(pictureSize)
				.align(Alignment.Center)
		)

		VideoPlayerOverlay(
			playbackManager = playbackManager,
			mediaToastRegistry = mediaToastRegistry,
		)

		PlayerSubtitles(
			playbackManager = playbackManager,
			modifier = Modifier
				.size(subtitleSize)
				.align(Alignment.Center)
		)
	}
}
