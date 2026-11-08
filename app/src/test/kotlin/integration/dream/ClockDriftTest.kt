package org.jellyfin.androidtv.integration.dream.composable

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ClockDriftTest : FunSpec({
	val loop = (0 until 24).map(::clockDriftOffset)

	test("the clock stays within the drift area") {
		loop.forEach { (x, y) ->
			x shouldBeInRange -120..120
			y shouldBeInRange -60..60
		}
	}

	test("the clock moves every minute") {
		(0 until 48).forEach { step -> clockDriftOffset(step + 1) shouldNotBe clockDriftOffset(step) }
	}

	test("it visits 24 places before the loop repeats") {
		loop.toSet() shouldHaveSize 24
		clockDriftOffset(24) shouldBe clockDriftOffset(0)
	}

	test("it reaches both sides of the area, not just the centre") {
		loop.minOf { it.first } shouldBe -120
		loop.maxOf { it.first } shouldBe 120
		loop.minOf { it.second } shouldBe -60
		loop.maxOf { it.second } shouldBe 60
	}
})
