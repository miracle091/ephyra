package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jellyfin.androidtv.integration.dream.model.DreamContent

@Composable
fun DreamView(
	content: DreamContent,
	showClock: Boolean,
) = Box(
	modifier = Modifier
		.fillMaxSize()
) {
	AnimatedContent(
		targetState = content,
		transitionSpec = {
			fadeIn(tween(durationMillis = 1_000)) togetherWith fadeOut(snap(delayMillis = 1_000))
		},
		label = "DreamContentTransition"
	) { content ->
		when (content) {
			DreamContent.Clock -> DreamContentClock() // Ephyra
			is DreamContent.NowPlaying -> DreamContentNowPlaying(content)
			is DreamContent.VideoPaused -> DreamContentPaused(content) // Ephyra
		}
	}

	// Header overlay
	DreamHeader(
		// Ephyra: the clock screen is the clock already; the long pause screen always shows it, as the player does
		showClock = content is DreamContent.VideoPaused || (showClock && content !is DreamContent.Clock),
	)
}
