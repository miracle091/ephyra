package org.jellyfin.androidtv.ui.livetv

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class GuideStyleTest : FunSpec({
	val start = LocalDateTime.of(2026, 11, 8, 20, 0)
	val end = LocalDateTime.of(2026, 11, 8, 21, 0)

	test("A programme on air has run by the share of its length") {
		GuideStyle.nowProgress(start, end, start, start.plusMinutes(15)) shouldBe 250
	}

	test("A programme that is not on air has no progress") {
		GuideStyle.nowProgress(start, end, start, start.minusMinutes(1)) shouldBe -1
		GuideStyle.nowProgress(start, end, start, end) shouldBe -1
	}

	test("A programme cropped by the guide's start is measured over the part that shows") {
		GuideStyle.nowProgress(start, end, start.plusMinutes(30), start.plusMinutes(45)) shouldBe 500
	}
})
