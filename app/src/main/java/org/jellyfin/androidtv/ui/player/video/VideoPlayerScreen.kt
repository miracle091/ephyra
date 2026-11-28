package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ZoomMode
import org.jellyfin.androidtv.ui.ScreensaverLock
import org.jellyfin.androidtv.ui.player.base.PlayerSubtitles
import org.jellyfin.androidtv.ui.player.base.PlayerSurface
import org.jellyfin.androidtv.ui.player.base.toast.LocalMediaToastRegistry
import org.jellyfin.androidtv.ui.player.base.toast.MediaToastRegistry
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.QueueService
import org.koin.compose.koinInject

private const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f

@Composable
fun VideoPlayerScreen(
	// Velella: the prompt asked while the queue holds at the end of an entry (VideoPlayerFragment sets the hold)
	endPrompt: StateFlow<EndOfEntryPrompt>,
	// Velella: tunes to another live TV channel (channel bar, guide, previous channel)
	liveTvSwitch: LiveTvSwitch,
) {
	val playbackManager = koinInject<PlaybackManager>()
	val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
	val close = { backDispatcher?.onBackPressed() ?: Unit }
	VideoPlayerExits(playbackManager, onClose = close, switchingChannel = liveTvSwitch.switching)
	val queueService = remember(playbackManager) { requireNotNull(playbackManager.getService<QueueService>()) { "No queue service" } }
	val heldNext by queueService.heldNext.collectAsState()
	val prompt by endPrompt.collectAsState()

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
	// Velella: shows the play and pause icons of the user's own commands (PlayPauseCommand.kt) and the quick seek steps
	val mediaToastRegistry = remember { MediaToastRegistry(coroutineScope) }

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

		PlayerSubtitles(
			playbackManager = playbackManager,
			modifier = Modifier
				.size(subtitleSize)
				.align(Alignment.Center)
		)

		// Velella: next up or still watching replace the controls while the queue holds (the controls would take the focus back)
		val next = heldNext
		when (playerOverlay(prompt, holding = next != null)) {
			// Velella: the controls' play/pause button shows its icons through the registry too
			PlayerOverlay.CONTROLS -> CompositionLocalProvider(LocalMediaToastRegistry provides mediaToastRegistry) {
				VideoPlayerOverlay(
					playbackManager = playbackManager,
					mediaToastRegistry = mediaToastRegistry,
					liveTvSwitch = liveTvSwitch,
				)
			}

			PlayerOverlay.END_OF_ENTRY -> if (next != null) EndOfEntryOverlay(playbackManager, prompt, next, onClose = close)
			PlayerOverlay.NONE -> Unit
		}
	}
}
