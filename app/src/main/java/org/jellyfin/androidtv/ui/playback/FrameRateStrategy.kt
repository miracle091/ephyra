package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior

/**
 * Ephyra: when the app switches the display mode itself (PlaybackController), Media3 must not also ask the surface
 * for a frame rate, or the two requests can fight. With switching disabled, Media3 keeps its own default.
 */
@OptIn(UnstableApi::class)
fun videoChangeFrameRateStrategy(behavior: RefreshRateSwitchingBehavior): Int = when (behavior) {
	RefreshRateSwitchingBehavior.DISABLED -> C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS
	RefreshRateSwitchingBehavior.SCALE_ON_TV,
	RefreshRateSwitchingBehavior.SCALE_ON_DEVICE -> C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF
}
