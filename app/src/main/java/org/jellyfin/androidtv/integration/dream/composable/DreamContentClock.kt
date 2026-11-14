package org.jellyfin.androidtv.integration.dream.composable

import android.text.format.DateFormat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.ui.base.EphyraTheme
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.rememberCurrentTime
import org.jellyfin.androidtv.util.locale
import kotlin.time.Duration.Companion.minutes

private val ClockDriftInterval = 1.minutes
private const val CLOCK_DRIFT_MS = 4_000

// How far the clock wanders from the centre, against burn-in on OLED and plasma screens
private const val CLOCK_DRIFT_X_DP = 120
private const val CLOCK_DRIFT_Y_DP = 60
private const val CLOCK_DRIFT_X_STRIDE = 3
private const val CLOCK_DRIFT_X_POINTS = 8
private const val CLOCK_DRIFT_Y_STRIDE = 5
private const val CLOCK_DRIFT_Y_POINTS = 12

/**
 * Ephyra: where the clock sits at the given minute, as an offset from the centre in dp. It walks a fixed loop of points spread
 * over the whole drift area, so no pixel keeps the same content for long and two consecutive points are never the same.
 */
fun clockDriftOffset(step: Int): Pair<Int, Int> {
	// Steps of 3/8 and 5/12 of the way on each axis: 24 different points (24 minutes) before the loop repeats
	val x = driftOnAxis(step, CLOCK_DRIFT_X_DP, stride = CLOCK_DRIFT_X_STRIDE, points = CLOCK_DRIFT_X_POINTS)
	val y = driftOnAxis(step, CLOCK_DRIFT_Y_DP, stride = CLOCK_DRIFT_Y_STRIDE, points = CLOCK_DRIFT_Y_POINTS)
	return x.toInt() to y.toInt()
}

/** One axis of the drift: [points] positions spread evenly between -[amplitude] and +[amplitude], visited [stride] at a time. */
private fun driftOnAxis(step: Int, amplitude: Int, stride: Int, points: Int): Float {
	val half = (points - 1) / 2f
	return amplitude * (((step * stride) % points) - half) / half
}

/** Ephyra: the screensaver: the theme's background with a large clock and the date, moving slowly. */
@Composable
fun DreamContentClock() = EphyraTheme {
	val palette = LocalEphyraPalette.current
	val context = LocalContext.current
	val datePattern = remember(context) { DateFormat.getBestDateTimePattern(context.locale, "EEEEdMMMM") }
	val time by rememberCurrentTime()
	val date by rememberCurrentTime(format12 = datePattern, format24 = datePattern)

	var step by remember { mutableIntStateOf(0) }
	LaunchedEffect(Unit) {
		while (true) {
			delay(ClockDriftInterval)
			step++
		}
	}
	val (driftX, driftY) = clockDriftOffset(step)
	val offsetX by animateDpAsState(driftX.dp, tween(CLOCK_DRIFT_MS), label = "ClockDriftX")
	val offsetY by animateDpAsState(driftY.dp, tween(CLOCK_DRIFT_MS), label = "ClockDriftY")

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(palette.bg),
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
			modifier = Modifier
				.align(Alignment.Center)
				.offset(x = offsetX, y = offsetY),
		) {
			Text(
				text = time,
				style = LocalTextStyle.current.merge(
					color = palette.ink,
					fontSize = 128.sp,
					fontWeight = FontWeight.Light,
					lineHeight = 132.sp,
				),
			)
			Text(
				text = date,
				style = LocalTextStyle.current.merge(
					color = palette.inkMuted,
					fontSize = 24.sp,
					fontWeight = FontWeight.Medium,
				),
			)
		}
	}
}
