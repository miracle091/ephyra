package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior

class DisplayModeScoringTest : FunSpec({
	fun difference(behavior: RefreshRateSwitchingBehavior, modeWidth: Int, modeHeight: Int) =
		displayModeResolutionDifference(behavior, DisplaySize(modeWidth, modeHeight), DisplaySize(3840, 2160), 1920)

	test("SCALE_ON_DEVICE scores the native resolution 0") {
		difference(RefreshRateSwitchingBehavior.SCALE_ON_DEVICE, 3840, 2160) shouldBe 0
	}

	test("SCALE_ON_DEVICE penalises other resolutions more than any refresh rate difference") {
		val penalty = difference(RefreshRateSwitchingBehavior.SCALE_ON_DEVICE, 1920, 1080)
		penalty shouldBe NON_NATIVE_RESOLUTION_PENALTY
		// 12000 is the largest rate (120 Hz) the weight has to account for
		penalty shouldBeGreaterThan 12_000
	}

	test("SCALE_ON_DEVICE keeps the native mode at 24 Hz over an exact 23.976 Hz 1080p mode") {
		fun weight(behavior: RefreshRateSwitchingBehavior, width: Int, height: Int, rate: Int) =
			100_000 - (rate - 2398) + 100_000 - difference(behavior, width, height)

		val native = weight(RefreshRateSwitchingBehavior.SCALE_ON_DEVICE, 3840, 2160, 2400)
		val lower = weight(RefreshRateSwitchingBehavior.SCALE_ON_DEVICE, 1920, 1080, 2398)
		native shouldBeGreaterThan lower
	}

	test("SCALE_ON_DEVICE prefers the non-native width closest to the video when the native mode is out") {
		// Native 1080p, 4K media: the native mode is excluded upstream of the scoring
		fun score(modeWidth: Int) = displayModeResolutionDifference(
			RefreshRateSwitchingBehavior.SCALE_ON_DEVICE, DisplaySize(modeWidth, 2160), DisplaySize(1920, 1080), 3840,
		)
		score(4096) shouldBeGreaterThan score(3840)
	}

	test("SCALE_ON_TV scores by distance from the video width") {
		difference(RefreshRateSwitchingBehavior.SCALE_ON_TV, 1920, 1080) shouldBe 0
		difference(RefreshRateSwitchingBehavior.SCALE_ON_TV, 3840, 2160) shouldBe 1920
	}
})
