package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior
import kotlin.math.abs

/**
 * Ephyra: penalty of a non-native resolution when scaling on the device. It must outweigh any refresh rate difference
 * (the weight in PlaybackController subtracts both), so a native resolution mode always beats a lower resolution one
 * (upstream #5893).
 */
const val NON_NATIVE_RESOLUTION_PENALTY = 50_000

/**
 * Resolution part of the display mode score, lower is better.
 * - SCALE_ON_DEVICE: 0 for the native (current) resolution, [NON_NATIVE_RESOLUTION_PENALTY] plus the distance from the video
 *   width for any other.
 * - SCALE_ON_TV: distance between the mode width and the video width.
 * - DISABLED: 0.
 */
fun displayModeResolutionDifference(
	behavior: RefreshRateSwitchingBehavior,
	modeWidth: Int,
	modeHeight: Int,
	nativeWidth: Int,
	nativeHeight: Int,
	videoWidth: Int,
): Int = when (behavior) {
	RefreshRateSwitchingBehavior.SCALE_ON_DEVICE ->
		// Among non-native modes (native excluded, e.g. a 1080p native mode for 4K media) the width closest to the video wins
		if (modeWidth != nativeWidth || modeHeight != nativeHeight) NON_NATIVE_RESOLUTION_PENALTY + abs(modeWidth - videoWidth) else 0
	RefreshRateSwitchingBehavior.SCALE_ON_TV -> abs(modeWidth - videoWidth)
	RefreshRateSwitchingBehavior.DISABLED -> 0
}
