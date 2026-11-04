package org.jellyfin.androidtv.ui.playback.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/** Ephyra: how long the skip button stays fully visible before it dims. */
internal val SkipDimDelay = 3.seconds

/** Ephyra: the skip button's opacity once dimmed, until it hides. */
internal const val SKIP_DIMMED_ALPHA = 0.4f

private const val SKIP_DIM_FADE_MS = 600

/**
 * Ephyra: opacity of the skip button: fully visible when it appears, dimmed after [SkipDimDelay] so it stays out of the way
 * until it hides by itself.
 */
@Composable
fun skipButtonAlpha(visible: Boolean): Float {
	var dimmed by remember { mutableStateOf(false) }
	LaunchedEffect(visible) {
		dimmed = false
		if (visible) {
			delay(SkipDimDelay)
			dimmed = true
		}
	}

	val alpha by animateFloatAsState(
		targetValue = if (dimmed) SKIP_DIMMED_ALPHA else 1f,
		animationSpec = tween(SKIP_DIM_FADE_MS),
		label = "skip button alpha",
	)
	return alpha
}
