package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jellyfin.androidtv.ui.base.dialog.DialogBase
import org.jellyfin.androidtv.ui.base.dialog.DialogDefaults

/** Duration of the slide and fade transitions of the settings, in milliseconds. */
internal const val SETTINGS_TRANSITION_DURATION_MILLIS = 400

@Composable
fun SettingsDialog(
	visible: Boolean,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	screen: @Composable BoxScope.() -> Unit,
) {
	DialogBase(
		visible = visible,
		onDismissRequest = onDismissRequest,
		modifier = modifier,
		style = DialogDefaults.style(
			contentAlignment = Alignment.TopEnd,
			enterTransition = slideInHorizontally(
				initialOffsetX = { it },
				animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS, easing = FastOutSlowInEasing)
			) + fadeIn(
				animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS)
			),
			exitTransition = slideOutHorizontally(
				targetOffsetX = { it },
				animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS, easing = FastOutSlowInEasing)
			) + fadeOut(
				animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS)
			),
		),
	) {
		SettingsLayout {
			screen()
		}
	}
}
