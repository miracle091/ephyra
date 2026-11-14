package org.jellyfin.androidtv.ui.base.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.interactionTracker

private const val CONTENT_TRANSITION_MILLIS = 300
private const val SCRIM_TRANSITION_MILLIS = 1000

/** How a dialog looks: the [scrimColor] behind it, how its content enters and exits, and where it sits. */
@Immutable
data class DialogStyle(
	val scrimColor: Color,
	val enterTransition: EnterTransition,
	val exitTransition: ExitTransition,
	val contentAlignment: Alignment,
)

object DialogDefaults {
	@Composable
	fun style(
		scrimColor: Color = JellyfinTheme.colorScheme.scrim,
		enterTransition: EnterTransition = fadeIn(tween(CONTENT_TRANSITION_MILLIS)),
		exitTransition: ExitTransition = fadeOut(tween(CONTENT_TRANSITION_MILLIS)),
		contentAlignment: Alignment = Alignment.Center,
	) = DialogStyle(
		scrimColor = scrimColor,
		enterTransition = enterTransition,
		exitTransition = exitTransition,
		contentAlignment = contentAlignment,
	)
}

@Composable
fun DialogBase(
	visible: Boolean,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	style: DialogStyle = DialogDefaults.style(),
	content: @Composable BoxScope.() -> Unit,
) {
	val transition = updateTransition(visible)
	val alpha by transition.animateFloat(
		transitionSpec = { tween(SCRIM_TRANSITION_MILLIS) },
		targetValueByState = { visible -> if (visible) 1f else 0f }
	)

	if (transition.currentState || transition.isRunning) {
		val focusRequester = remember { FocusRequester() }
		var contentVisible by remember { mutableStateOf(false) }

		Dialog(
			onDismissRequest = onDismissRequest,
			properties = DialogProperties(
				dismissOnBackPress = true,
				dismissOnClickOutside = true,
				usePlatformDefaultWidth = false,
				decorFitsSystemWindows = false
			),
		) {
			Box(
				modifier = modifier
					.fillMaxSize()
					.background(style.scrimColor.copy(alpha = style.scrimColor.alpha * alpha))
					.focusRequester(focusRequester)
					.focusGroup()
					.interactionTracker(),
				contentAlignment = style.contentAlignment,
			) {
				transition.AnimatedVisibility(
					visible = { it && contentVisible },
					enter = style.enterTransition,
					exit = style.exitTransition,
				) {
					content()
					LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
				}
			}
		}

		LaunchedEffect(contentVisible) { contentVisible = true }
	}
}
