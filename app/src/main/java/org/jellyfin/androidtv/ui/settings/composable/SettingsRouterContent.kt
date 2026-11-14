package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import org.jellyfin.androidtv.ui.navigation.RouterContent

/**
 * Wrapper for the [RouterContent] composable that sets the settings slide in/out transitions.
 */
@Composable
fun SettingsRouterContent() {
	val transitionIn = slideInHorizontally(
		initialOffsetX = { it },
		animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS, easing = FastOutSlowInEasing)
	) + fadeIn(
		animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS)
	)
	val transitionOut = slideOutHorizontally(
		targetOffsetX = { -it / 3 },
		animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS, easing = FastOutSlowInEasing)
	) + fadeOut(
		animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS)
	)

	val popIn = slideInHorizontally(
		initialOffsetX = { -it / 3 },
		animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS, easing = FastOutSlowInEasing)
	) + fadeIn(
		animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS)
	)

	val popOut = slideOutHorizontally(
		targetOffsetX = { it },
		animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS, easing = FastOutSlowInEasing)
	) + fadeOut(
		animationSpec = tween(SETTINGS_TRANSITION_DURATION_MILLIS)
	)

	RouterContent(
		transitionSpec = { transitionIn togetherWith transitionOut },
		popTransitionSpec = { popIn togetherWith popOut },
	)
}
