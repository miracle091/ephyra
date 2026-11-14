package org.jellyfin.androidtv.ui.screensaver

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import org.jellyfin.androidtv.integration.dream.composable.DreamHost
import org.jellyfin.androidtv.ui.InteractionTrackerViewModel
import org.jellyfin.androidtv.ui.base.dialog.DialogBase
import org.jellyfin.androidtv.ui.base.dialog.DialogDefaults
import org.jellyfin.androidtv.util.isMediaSessionKeyEvent
import org.koin.androidx.compose.koinViewModel

private const val FADE_MILLIS = 1_000

@Composable
fun InAppScreensaver() {
	val interactionTrackerViewModel = koinViewModel<InteractionTrackerViewModel>()
	val visible by interactionTrackerViewModel.visible.collectAsState()

	DialogBase(
		visible = visible,
		onDismissRequest = {
			interactionTrackerViewModel.notifyInteraction(canCancel = true, userInitiated = false)
		},
		style = DialogDefaults.style(
			scrimColor = Color.Black,
			enterTransition = fadeIn(tween(FADE_MILLIS)),
			exitTransition = fadeOut(tween(FADE_MILLIS)),
		),
		modifier = Modifier
			.fillMaxSize()
			.clickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
			) {
				interactionTrackerViewModel.notifyInteraction(canCancel = true, userInitiated = false)
			}
			.onKeyEvent { event ->
				if (!event.nativeKeyEvent.isMediaSessionKeyEvent()) {
					interactionTrackerViewModel.notifyInteraction(
						canCancel = event.type == KeyEventType.KeyUp,
						userInitiated = true,
					)
				}
				false
			}
	) {
		DreamHost()
	}
}
