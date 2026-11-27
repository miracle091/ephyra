package org.jellyfin.androidtv.ui.livetv

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class GuideStyleTest : FunSpec({
	val start = LocalDateTime.of(2026, 11, 8, 20, 0)
	val end = LocalDateTime.of(2026, 11, 8, 21, 0)

	test("A programme on air has run by the share of its length") {
		GuideStyle.nowProgress(start, end, start.plusMinutes(15)) shouldBe 250
	}

	test("A programme that is not on air has no progress") {
		GuideStyle.nowProgress(start, end, start.minusMinutes(1)) shouldBe -1
		GuideStyle.nowProgress(start, end, end) shouldBe -1
	}

	test("The progress advances with the clock and the next programme takes over at the boundary") {
		val next = end.plusHours(1)
		GuideStyle.nowProgress(start, end, start.plusMinutes(30)) shouldBe 500
		GuideStyle.nowProgress(start, end, start.plusMinutes(31)) shouldBe 516
		GuideStyle.nowProgress(start, end, end) shouldBe -1
		GuideStyle.nowProgress(end, next, end) shouldBe 0
	}

	test("A programme that began before the guide's start is measured over its whole length") {
		GuideStyle.nowProgress(start, end, start.plusMinutes(45)) shouldBe 750
	}

	test("The cell of a programme that began before the guide's start begins at the guide's start, so its bar ends on now") {
		val guideStart = start.plusMinutes(30)
		val visible = GuideStyle.visibleStart(start, guideStart)
		visible shouldBe guideStart
		// 45 of 60 minutes have run, but only 15 of the 30 visible ones
		GuideStyle.nowProgress(visible, end, start.plusMinutes(45)) shouldBe 500
		GuideStyle.visibleStart(start.plusMinutes(40), guideStart) shouldBe start.plusMinutes(40)
	}

	test("The minutes left round up, so a programme on air never shows zero") {
		GuideStyle.minutesLeft(end, end.minusMinutes(27)) shouldBe 27
		GuideStyle.minutesLeft(end, end.minusSeconds(30)) shouldBe 1
		GuideStyle.minutesLeft(end, end.minusMinutes(26).minusSeconds(1)) shouldBe 27
		GuideStyle.minutesLeft(end, end) shouldBe 0
	}
})
