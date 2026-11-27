package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.shared.toolbar.ToolbarClock

@Composable
fun DreamHeader(
	showClock: Boolean,
) = Box(
	modifier = Modifier
		.fillMaxWidth()
		// Velella: the home toolbar's measures, so the clock stays where it was when the screensaver starts
		.height(95.dp)
		.padding(horizontal = 24.dp, vertical = 27.dp),
) {
	// Clock
	AnimatedVisibility(
		visible = showClock,
		enter = fadeIn(),
		exit = fadeOut(),
		modifier = Modifier
			.align(Alignment.CenterEnd),
	) {
		ToolbarClock()
	}
}
