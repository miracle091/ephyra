package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LiveStreamRetryTest : FunSpec({
	test("a live stream is retried three times") {
		LiveStreamRetry.shouldRetry(isLiveTv = true, attemptsDone = 0) shouldBe true
		LiveStreamRetry.shouldRetry(isLiveTv = true, attemptsDone = 2) shouldBe true
		LiveStreamRetry.shouldRetry(isLiveTv = true, attemptsDone = 3) shouldBe false
	}

	test("anything but live TV is never retried here") {
		LiveStreamRetry.shouldRetry(isLiveTv = false, attemptsDone = 0) shouldBe false
	}

	test("the pause grows with each retry") {
		LiveStreamRetry.delayMs(1) shouldBe 1000
		LiveStreamRetry.delayMs(2) shouldBe 2000
		LiveStreamRetry.delayMs(0) shouldBe 1000
	}
})
