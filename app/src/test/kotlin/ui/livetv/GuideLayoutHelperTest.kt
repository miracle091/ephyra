package org.jellyfin.androidtv.ui.livetv

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class GuideLayoutHelperTest : FunSpec({
	fun at(hour: Int, minute: Int, second: Int = 0) = LocalDateTime.of(2026, 11, 27, hour, minute, second)

	test("the guide starts at the half hour now falls in") {
		GuideLayoutHelper.guideStart(at(22, 27, 41)) shouldBe at(22, 0)
		GuideLayoutHelper.guideStart(at(22, 41)) shouldBe at(22, 30)
		GuideLayoutHelper.guideStart(at(22, 30)) shouldBe at(22, 30)
	}

	test("the first half-hour label comes after the minutes left to the next mark") {
		GuideLayoutHelper.minutesToNextHalfHour(at(22, 22)) shouldBe 8
		GuideLayoutHelper.minutesToNextHalfHour(at(22, 30)) shouldBe 0
		GuideLayoutHelper.minutesToNextHalfHour(at(22, 45)) shouldBe 15
	}
})
