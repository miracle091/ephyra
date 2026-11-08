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
})
