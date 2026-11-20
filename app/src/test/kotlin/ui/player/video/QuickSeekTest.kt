package org.jellyfin.androidtv.ui.player.video

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class QuickSeekTest : FunSpec({
	val forward = 30.seconds
	val rewind = 10.seconds
	val duration = 40.minutes

	test("each press moves the target by the user's fast forward or rewind amount") {
		quickSeekTarget(5.minutes, forward = true, forward, rewind, duration) shouldBe 5.minutes + 30.seconds
		quickSeekTarget(5.minutes, forward = false, forward, rewind, duration) shouldBe 4.minutes + 50.seconds
	}

	test("the target stays between the start and the end") {
		quickSeekTarget(5.seconds, forward = false, forward, rewind, duration) shouldBe Duration.ZERO
		quickSeekTarget(duration - 10.seconds, forward = true, forward, rewind, duration) shouldBe duration
	}

	test("the label shows the distance from where the quick seek started") {
		quickSeekLabel(90.seconds) shouldBe "+1:30"
		quickSeekLabel((-20).seconds) shouldBe "−0:20"
		quickSeekLabel(Duration.ZERO) shouldBe "+0:00"
		quickSeekLabel(65.minutes + 5.seconds) shouldBe "+1:05:05"
	}
})
