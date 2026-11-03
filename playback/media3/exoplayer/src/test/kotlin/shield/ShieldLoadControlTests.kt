package org.jellyfin.playback.media3.exoplayer.shield

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val MB = 1024 * 1024

class ShieldLoadControlTests : FunSpec({
	test("Target buffer is a quarter of the large heap") {
		ShieldLoadControl.targetBufferBytes(largeMemoryClassMb = 512) shouldBe 128 * MB
	}

	test("Target buffer never drops below 48 MB on a small heap") {
		ShieldLoadControl.targetBufferBytes(largeMemoryClassMb = 128) shouldBe 48 * MB
	}

	test("Target buffer never grows above 192 MB on a large heap") {
		ShieldLoadControl.targetBufferBytes(largeMemoryClassMb = 2048) shouldBe 192 * MB
	}
})
