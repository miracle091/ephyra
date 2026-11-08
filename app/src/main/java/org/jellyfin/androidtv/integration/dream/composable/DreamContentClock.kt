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
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.rememberCurrentTime
import org.jellyfin.androidtv.util.locale
import kotlin.time.Duration.Companion.minutes

private val ClockDriftInterval = 1.minutes
private const val CLOCK_DRIFT_MS = 4_000

// How far the clock wanders from the centre, against burn-in on OLED and plasma screens
private const val CLOCK_DRIFT_X_DP = 120
private const val CLOCK_DRIFT_Y_DP = 60

/**
 * Ephyra: where the clock sits at the given minute, as an offset from the centre in dp. It walks a fixed loop of points spread
 * over the whole drift area, so no pixel keeps the same content for long and two consecutive points are never the same.
 */
fun clockDriftOffset(step: Int): Pair<Int, Int> {
	// Steps of 3/8 and 5/12 of the way on each axis: 24 different points (24 minutes) before the loop repeats
	val x = CLOCK_DRIFT_X_DP * (((step * 3) % 8) - 3.5f) / 3.5f
	val y = CLOCK_DRIFT_Y_DP * (((step * 5) % 12) - 5.5f) / 5.5f
	return x.toInt() to y.toInt()
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
				fontSize = 128.sp,
				lineHeight = 132.sp,
				fontWeight = FontWeight.Light,
				color = palette.ink,
			)
			Text(
				text = date,
				fontSize = 24.sp,
				fontWeight = FontWeight.Medium,
				color = palette.inkMuted,
			)
		}
	}
}
