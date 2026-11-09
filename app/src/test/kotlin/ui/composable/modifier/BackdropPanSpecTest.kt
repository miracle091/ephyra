package org.jellyfin.androidtv.ui.composable.modifier

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.shouldBe

class BackdropPanSpecTest : FunSpec({
	test("direction alternates between neighbouring items, also for negative indexes") {
		BackdropPanSpec.direction(0) shouldBe 1
		BackdropPanSpec.direction(1) shouldBe -1
		BackdropPanSpec.direction(2) shouldBe 1
		BackdropPanSpec.direction(-1) shouldBe -1
	}

	test("the pan is off when the animator duration scale is 0") {
		BackdropPanSpec.isEnabled(0f) shouldBe false
		BackdropPanSpec.isEnabled(1f) shouldBe true
		BackdropPanSpec.isEnabled(0.5f) shouldBe true
	}

	test("scale goes from 1 to the end scale and stays in range") {
		BackdropPanSpec.scale(0f) shouldBeExactly 1f
		BackdropPanSpec.scale(1f) shouldBeExactly BackdropPanSpec.END_SCALE
		BackdropPanSpec.scale(2f) shouldBeExactly BackdropPanSpec.END_SCALE
		BackdropPanSpec.scale(-1f) shouldBeExactly 1f
	}

	test("remaining time shrinks with the progress") {
		BackdropPanSpec.remainingMs(0f) shouldBe BackdropPanSpec.DURATION_MS
		BackdropPanSpec.remainingMs(0.5f) shouldBe BackdropPanSpec.DURATION_MS / 2
		BackdropPanSpec.remainingMs(1f) shouldBe 0
	}
})
