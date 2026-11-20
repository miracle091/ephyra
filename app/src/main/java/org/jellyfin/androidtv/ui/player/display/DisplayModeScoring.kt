package org.jellyfin.androidtv.ui.player.display

import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Ephyra: penalty of a non-native resolution when scaling on the device. It must outweigh any refresh rate difference
 * (the weight in [findBestDisplayMode] subtracts both), so a native resolution mode always beats a lower resolution one
 * (upstream #5893).
 */
const val NON_NATIVE_RESOLUTION_PENALTY = 50_000

/** Ephyra: the pixel size of a display mode. */
data class DisplaySize(val width: Int, val height: Int)

/**
 * Resolution part of the display mode score, lower is better.
 * - SCALE_ON_DEVICE: 0 for the native (current) resolution, [NON_NATIVE_RESOLUTION_PENALTY] plus the distance from the video
 *   width for any other.
 * - SCALE_ON_TV: distance between the mode width and the video width.
 * - DISABLED: 0.
 */
fun displayModeResolutionDifference(
	behavior: RefreshRateSwitchingBehavior,
	mode: DisplaySize,
	native: DisplaySize,
	videoWidth: Int,
): Int = when (behavior) {
	RefreshRateSwitchingBehavior.SCALE_ON_DEVICE ->
		// Among non-native modes (native excluded, e.g. a 1080p native mode for 4K media) the width closest to the video wins
		if (mode != native) NON_NATIVE_RESOLUTION_PENALTY + abs(mode.width - videoWidth) else 0
	RefreshRateSwitchingBehavior.SCALE_ON_TV -> abs(mode.width - videoWidth)
	RefreshRateSwitchingBehavior.DISABLED -> 0
}

/** Ephyra: a display mode as the scoring sees it ([android.view.Display.Mode] without the Android dependency). */
data class DisplayModeInfo(val id: Int, val size: DisplaySize, val refreshRate: Float)

/** Ephyra: the video a display mode is picked for, from the source's video stream. */
data class VideoModeRequest(val width: Int, val height: Int, val frameRate: Float)

// 720p: smaller modes are never picked
private const val MIN_MODE_WIDTH = 1280
private const val MIN_MODE_HEIGHT = 720
private val MIN_MODE_SIZE = DisplaySize(MIN_MODE_WIDTH, MIN_MODE_HEIGHT)

// Rates are compared in hundredths of a hertz; the weight base leaves room for rates of 120 Hz and more (12 000)
private const val RATE_SCALE = 100
private const val WEIGHT_BASE = 100_000
private const val DOUBLE_RATE = 2
private const val PULLDOWN_RATE = 2.5

/**
 * Ephyra: the display mode to play [video] in, shared by both players, or null when no mode fits. A mode qualifies when it is
 * HD or better, not smaller than the video, and its refresh rate is the video's frame rate, twice it or 2.5 times it (3:2
 * pulldown); among those the closest rate and resolution win (see [displayModeResolutionDifference]).
 */
fun findBestDisplayMode(
	modes: List<DisplayModeInfo>,
	current: DisplayModeInfo,
	video: VideoModeRequest,
	behavior: RefreshRateSwitchingBehavior,
): DisplayModeInfo? {
	val sourceRate = (video.frameRate * RATE_SCALE).roundToInt()
	val wantedRates = setOf(sourceRate, sourceRate * DOUBLE_RATE, (sourceRate * PULLDOWN_RATE).roundToInt())

	return modes
		.filter { it.size.width >= MIN_MODE_SIZE.width && it.size.height >= MIN_MODE_SIZE.height }
		.filter { it.size.width >= video.width && it.size.height >= video.height }
		.filter { (it.refreshRate * RATE_SCALE).roundToInt() in wantedRates }
		.map { mode ->
			val rateDifference = (mode.refreshRate * RATE_SCALE).roundToInt() - sourceRate
			val resolutionDifference = displayModeResolutionDifference(behavior, mode.size, current.size, video.width)
			mode to (WEIGHT_BASE - rateDifference + WEIGHT_BASE - resolutionDifference)
		}
		.filter { (_, weight) -> weight > 0 }
		// The first of equal weights wins, as in the legacy loop
		.maxByOrNull { (_, weight) -> weight }
		?.first
}
