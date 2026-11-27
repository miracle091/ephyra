package org.jellyfin.androidtv.ui.home.spotlight

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SpotlightRotationTest : FunSpec({
	test("an item starts with its whole time and, paused by the focus, goes on with what was left") {
		remainingRotation(0f, 12.seconds) shouldBe 12.seconds
		remainingRotation(0.25f, 12.seconds) shouldBe 9.seconds
	}

	test("a full or out-of-range progress leaves nothing or the whole time") {
		remainingRotation(1f, 12.seconds) shouldBe Duration.ZERO
		remainingRotation(-1f, 12.seconds) shouldBe 12.seconds
	}
})
