package org.jellyfin.androidtv.ui.playback.overlay

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Ephyra: the choices for how long the skip button stays on screen, in seconds; 0 keeps it until the segment ends. */
val SkipAutoHideChoices = listOf(5, 10, 15, 20, 30, 0)

/** Ephyra: how long the skip button stays on screen before it hides by itself, or null to keep it until the segment ends. */
fun skipAutoHideDelay(seconds: Int): Duration? = if (seconds > 0) seconds.seconds else null

/** Ephyra: whether the skip button is on screen, hidden but ready to be called back, or not there at all. */
enum class SkipButtonState { SHOWN, RECALLABLE, NONE }

/**
 * Ephyra: the skip button's state, like a streaming service's "Skip intro": shown inside a segment while the player's controls are
 * closed ([enabled]); once it hid by itself ([autoHidden]) it can be called back until the segment ends.
 */
fun skipButtonState(enabled: Boolean, autoHidden: Boolean, target: Duration?, position: Duration): SkipButtonState = when {
	!enabled || target == null || position > target - MediaSegmentRepository.SkipMinDuration -> SkipButtonState.NONE
	autoHidden -> SkipButtonState.RECALLABLE
	else -> SkipButtonState.SHOWN
}

/** Ephyra: the keys that call a hidden skip button back; left and right keep seeking. */
fun isSkipRecallKey(keyCode: Int): Boolean = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER ||
	keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN

/** Ephyra: how long the skip button stays fully visible before it dims. */
internal val SkipDimDelay = 3.seconds

/** Ephyra: the skip button's opacity once dimmed, until it hides. */
internal const val SKIP_DIMMED_ALPHA = 0.8f

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
