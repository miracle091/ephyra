package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LiveStreamRetryTest : FunSpec({
	test("a live stream is retried twice") {
		LiveStreamRetry.shouldRetry(isLiveTv = true, attemptsDone = 0) shouldBe true
		LiveStreamRetry.shouldRetry(isLiveTv = true, attemptsDone = 1) shouldBe true
		LiveStreamRetry.shouldRetry(isLiveTv = true, attemptsDone = 2) shouldBe false
	}

	test("anything but live TV is never retried here") {
		LiveStreamRetry.shouldRetry(isLiveTv = false, attemptsDone = 0) shouldBe false
	}

	test("the pause grows with each retry") {
		LiveStreamRetry.delayMs(1) shouldBe 1000
		LiveStreamRetry.delayMs(2) shouldBe 2000
		LiveStreamRetry.delayMs(0) shouldBe 1000
	}

	test("a live stream gets 20 seconds to leave buffering, anything else no timeout") {
		LiveStreamRetry.bufferTimeoutMs(isLiveTv = true) shouldBe 20_000
		LiveStreamRetry.bufferTimeoutMs(isLiveTv = false) shouldBe 0
	}

	test("the timeout fails only a live stream that is still buffering") {
		LiveStreamRetry.shouldFailOnBufferTimeout(isLiveTv = true, isBuffering = true) shouldBe true
		LiveStreamRetry.shouldFailOnBufferTimeout(isLiveTv = true, isBuffering = false) shouldBe false
		LiveStreamRetry.shouldFailOnBufferTimeout(isLiveTv = false, isBuffering = true) shouldBe false
	}
})
