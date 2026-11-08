package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class LiveProgramTest : FunSpec({
	val start = LocalDateTime.of(2026, 11, 8, 20, 45)
	val end = LocalDateTime.of(2026, 11, 8, 22, 45)

	test("duration is the programme length") {
		LiveProgram.durationMs(start, end) shouldBe 2 * 60 * 60 * 1000L
	}

	test("duration is unknown without times or with an end before the start") {
		LiveProgram.durationMs(null, end) shouldBe -1
		LiveProgram.durationMs(start, null) shouldBe -1
		LiveProgram.durationMs(end, start) shouldBe -1
	}

	test("position is the elapsed share") {
		LiveProgram.positionMs(start, end, start.plusMinutes(30)) shouldBe 30 * 60 * 1000L
	}

	test("position never goes past the programme") {
		LiveProgram.positionMs(start, end, end.plusMinutes(5)) shouldBe 2 * 60 * 60 * 1000L
		LiveProgram.positionMs(start, end, start.minusMinutes(5)) shouldBe 0
	}

	test("position is 0 when the programme is unknown") {
		LiveProgram.positionMs(null, null, start) shouldBe 0
	}

	test("a programme has ended from its end time on") {
		LiveProgram.hasEnded(end, end.minusSeconds(1)) shouldBe false
		LiveProgram.hasEnded(end, end) shouldBe true
		LiveProgram.hasEnded(null, end) shouldBe false
	}
})
