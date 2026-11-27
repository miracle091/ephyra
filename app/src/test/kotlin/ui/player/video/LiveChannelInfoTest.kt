package org.jellyfin.androidtv.ui.player.video

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.BaseItemDto
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LiveChannelInfoTest : FunSpec({
	val now = LocalDateTime.of(2026, 11, 27, 21, 0)

	test("waits until the programme on air ends") {
		liveRefreshDelay(now.plusMinutes(3), now) shouldBe 3.minutes
	}

	test("waits at least 15 seconds, also once the programme has ended") {
		liveRefreshDelay(now.plusSeconds(2), now) shouldBe 15.seconds
		liveRefreshDelay(now.minusMinutes(1), now) shouldBe 15.seconds
	}

	test("looks again after 5 minutes at most, or when the end is unknown") {
		liveRefreshDelay(now.plusHours(2), now) shouldBe 5.minutes
		liveRefreshDelay(null, now) shouldBe 5.minutes
	}

	fun program(end: LocalDateTime) = mockk<BaseItemDto> { every { endDate } returns end }

	test("the server's programme replaces the previous one, and a gap in the guide shows none") {
		val previous = program(now.plusMinutes(10))
		val loaded = program(now.plusHours(1))
		currentProgramAfter(Result.success(loaded), previous, now) shouldBe loaded
		currentProgramAfter(Result.success(null), previous, now) shouldBe null
	}

	test("a failed lookup keeps the previous programme only while it has not ended") {
		val failed = Result.failure<BaseItemDto?>(IllegalStateException("offline"))
		val airing = program(now.plusMinutes(10))
		currentProgramAfter(failed, airing, now) shouldBe airing
		currentProgramAfter(failed, program(now.minusMinutes(1)), now) shouldBe null
		currentProgramAfter(failed, null, now) shouldBe null
	}
})
