package org.jellyfin.androidtv.ui.playback

import androidx.media3.common.C
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior

class FrameRateStrategyTest : FunSpec({
	test("Refresh-rate switching is on by default and keeps the native resolution") {
		UserPreferences.refreshRateSwitchingBehavior.defaultValue shouldBe RefreshRateSwitchingBehavior.SCALE_ON_DEVICE
	}

	test("Media3 frame-rate requests are off while the app switches the display mode") {
		videoChangeFrameRateStrategy(RefreshRateSwitchingBehavior.SCALE_ON_DEVICE) shouldBe C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF
		videoChangeFrameRateStrategy(RefreshRateSwitchingBehavior.SCALE_ON_TV) shouldBe C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF
	}

	test("Media3 keeps its default when switching is disabled") {
		videoChangeFrameRateStrategy(RefreshRateSwitchingBehavior.DISABLED) shouldBe C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS
	}
})
