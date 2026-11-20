package org.jellyfin.androidtv.ui.player.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.composable.modifier.overscanPaddingValues
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val OVERLAY_SCRIM_ALPHA = 0.8f
private const val OVERLAY_HEIGHT_FRACTION = 1f / 3

@Composable
fun PlayerOverlayLayout(
	modifier: Modifier = Modifier,
	visibilityState: PlayerOverlayVisibilityState = rememberPlayerOverlayVisibility(),
	header: (@Composable () -> Unit)? = null,
	controls: (@Composable () -> Unit)? = null,
	// Ephyra: the video player keeps the legacy player's margins, the other players the default overscan
	contentPadding: PaddingValues = overscanPaddingValues,
) = Box(
	modifier = modifier
		.fillMaxSize()
		.focusable()
		.onPreviewKeyEvent {
			// Reset hide timer on key presses
			if (visibilityState.visible) visibilityState.show()

			// Otherwise, only act on key down
			if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

			if (it.key == Key.Back && visibilityState.visible) {
				visibilityState.hide()
				true
			} else if (!it.nativeKeyEvent.isSystem && !visibilityState.visible) {
				visibilityState.show()
				true
			} else {
				false
			}
		}
) {
	if (header != null) {
		AnimatedVisibility(
			visible = visibilityState.visible,
			modifier = Modifier
				.align(Alignment.TopCenter),
			enter = slideInVertically() + fadeIn(),
			exit = slideOutVertically() + fadeOut(),
		) {
			PlayerOverlayHeader(contentPadding, header)
		}
	}

	if (controls != null) {
		AnimatedVisibility(
			visible = visibilityState.visible,
			modifier = Modifier
				.align(Alignment.BottomCenter),
			enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
			exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
		) {
			PlayerOverlayControls(visibilityState, contentPadding, controls)
		}
	}
}

@Composable
private fun PlayerOverlayHeader(contentPadding: PaddingValues, header: @Composable () -> Unit) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.fillMaxHeight(OVERLAY_HEIGHT_FRACTION)
			.background(
				brush = Brush.verticalGradient(
					colors = listOf(
						Color.Black.copy(alpha = OVERLAY_SCRIM_ALPHA),
						Color.Transparent,
					)
				)
			)
			.padding(contentPadding)
	) {
		header()
	}
}

@Composable
private fun PlayerOverlayControls(
	visibilityState: PlayerOverlayVisibilityState,
	contentPadding: PaddingValues,
	controls: @Composable () -> Unit,
) {
	// Focus trap to detect when controls need to be closed by moving the focus up
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.height(0.dp)
			.focusTarget()
	)

	Box(
		contentAlignment = Alignment.BottomCenter,
		modifier = Modifier
			.fillMaxWidth()
			.fillMaxHeight(OVERLAY_HEIGHT_FRACTION)
			.background(
				brush = Brush.verticalGradient(
					colors = listOf(
						Color.Transparent,
						Color.Black.copy(alpha = OVERLAY_SCRIM_ALPHA),
					)
				)
			)
			.padding(contentPadding)
			.focusProperties {
				// Hide overlay when focus is moved out by going up
				onExit = {
					if (requestedFocusDirection == FocusDirection.Up) {
						Timber.i("Hide reason: focus moved up")
						visibilityState.hide()
						cancelFocusChange()
					}
				}
			},
	) {
		JellyfinTheme(
			colorScheme = JellyfinTheme.colorScheme.copy(
				button = Color.Transparent
			)
		) {
			controls()
		}
	}
}

data class PlayerOverlayVisibilityState(
	val visible: Boolean,

	val toggle: () -> Unit,
	val show: () -> Unit,
	val hide: () -> Unit,
)

@Composable
fun rememberPlayerOverlayVisibility(
	timeout: Duration = 5.seconds,
): PlayerOverlayVisibilityState {
	val scope = rememberCoroutineScope()
	var timerVisible by remember { mutableStateOf(false) }
	var timerJob by remember { mutableStateOf<Job?>(null) }

	fun show() {
		timerJob?.cancel()
		timerVisible = true
		timerJob = scope.launch {
			delay(timeout)
			timerVisible = false
		}
	}

	fun hide() {
		timerJob?.cancel()
		timerJob = null
		timerVisible = false
	}

	fun toggle() {
		if (timerVisible) hide()
		else show()
	}

	// Force visibility when not the active window, reset timer when it changes
	// to make sure popups keep the overlay visible
	val windowInfo = LocalWindowInfo.current
	val visible = timerVisible || !windowInfo.isWindowFocused

	var previousIsWindowFocused by remember { mutableStateOf(windowInfo.isWindowFocused) }
	LaunchedEffect(windowInfo.isWindowFocused) {
		if (windowInfo.isWindowFocused != previousIsWindowFocused) show()
		previousIsWindowFocused = windowInfo.isWindowFocused
	}

	return PlayerOverlayVisibilityState(
		visible = visible,
		toggle = ::toggle,
		show = ::show,
		hide = ::hide,
	)
}
