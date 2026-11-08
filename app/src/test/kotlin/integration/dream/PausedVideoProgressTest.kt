package org.jellyfin.androidtv.integration.dream

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PausedVideoProgressTest : FunSpec({
	test("remaining time and end time follow the position") {
		val progress = pausedVideoProgress(
			positionMs = 24.minutes.inWholeMilliseconds,
			durationMs = 45.minutes.inWholeMilliseconds,
			now = LocalTime.of(22, 10),
		)
		progress.remaining shouldBe 21.minutes
		progress.endsAt shouldBe LocalTime.of(22, 31)
		progress.fraction shouldBe 24f / 45f
	}

	test("a position past the end counts as the end") {
		val progress = pausedVideoProgress(positionMs = 50_000, durationMs = 40_000, now = LocalTime.NOON)
		progress.remaining shouldBe Duration.ZERO
		progress.fraction shouldBe 1f
	}

	test("an unknown duration shows no progress") {
		val progress = pausedVideoProgress(positionMs = 10_000, durationMs = 0, now = LocalTime.NOON)
		progress.fraction shouldBe 0f
		progress.endsAt shouldBe LocalTime.NOON
	}

	test("the end time wraps past midnight") {
		val progress = pausedVideoProgress(positionMs = 0, durationMs = 30.minutes.inWholeMilliseconds, now = LocalTime.of(23, 50))
		progress.endsAt shouldBe LocalTime.of(0, 20)
	}

	test("playback times read like the player's") {
		formatPlaybackTime(4.minutes.inWholeMilliseconds + 5.seconds.inWholeMilliseconds) shouldBe "4:05"
		formatPlaybackTime(24.minutes.inWholeMilliseconds + 20.seconds.inWholeMilliseconds) shouldBe "24:20"
		formatPlaybackTime(62.minutes.inWholeMilliseconds + 3.seconds.inWholeMilliseconds) shouldBe "1:02:03"
		formatPlaybackTime(-5) shouldBe "0:00"
	}
})
