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

	test("each filler of a channel without data lasts half an hour from the guide's start") {
		GuideLayoutHelper.fillerSlotEnd(at(23, 0), 0) shouldBe at(23, 30)
		GuideLayoutHelper.fillerSlotEnd(at(23, 0), 2) shouldBe at(0, 30).plusDays(1)
	}

	test("the page showing now falls behind when a new half hour begins, a later page never does") {
		GuideLayoutHelper.isBehindNow(at(23, 0), at(23, 29, 59)) shouldBe false
		GuideLayoutHelper.isBehindNow(at(23, 0), at(23, 30)) shouldBe true
		GuideLayoutHelper.isBehindNow(at(23, 30).plusDays(1), at(23, 45)) shouldBe false
	}

	test("the guide's ticker runs just after the next minute starts") {
		GuideLayoutHelper.delayToNextMinute(12 * 60_000L + 45_000L) shouldBe 15_100L
		GuideLayoutHelper.delayToNextMinute(12 * 60_000L) shouldBe 60_100L
	}

	test("a short cell grows to the minimum width and the next cell gives the space back") {
		GuideLayoutHelper.fitWidths(intArrayOf(20, 300, 200), 100).toList() shouldBe listOf(100, 220, 200)
	}

	test("short cells in a row share the borrowed space and the row keeps its width") {
		GuideLayoutHelper.fitWidths(intArrayOf(20, 20, 300), 100).toList() shouldBe listOf(100, 100, 140)
	}

	test("a short last cell takes the space from the cell before it") {
		GuideLayoutHelper.fitWidths(intArrayOf(300, 20), 100).toList() shouldBe listOf(220, 100)
	}

	test("cells already wide enough keep their widths") {
		GuideLayoutHelper.fitWidths(intArrayOf(150, 300), 100).toList() shouldBe listOf(150, 300)
	}
})
