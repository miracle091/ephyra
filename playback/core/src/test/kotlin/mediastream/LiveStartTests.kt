package org.jellyfin.playback.core.mediastream

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.jellyfin.playback.core.model.PlayState
import kotlin.time.Duration.Companion.seconds

class LiveStartTests : FunSpec({
	test("a live stream still buffering after the timeout stalled") {
		runTest {
			// Media3 reports a stream buffering at its start as paused
			liveStartStalled(MutableStateFlow(PlayState.PAUSED), 20.seconds) shouldBe true
		}
	}

	test("a live stream that starts playing in time did not stall") {
		runTest {
			val state = MutableStateFlow(PlayState.PAUSED)
			val stalled = async { liveStartStalled(state, 20.seconds) }
			advanceTimeBy(15.seconds)
			state.value = PlayState.PLAYING
			stalled.await() shouldBe false
		}
	}

	test("a live stream that failed is left to the retry chain") {
		runTest {
			val state = MutableStateFlow(PlayState.PAUSED)
			val stalled = async { liveStartStalled(state, 20.seconds) }
			advanceTimeBy(5.seconds)
			state.value = PlayState.ERROR
			stalled.await() shouldBe false
		}
	}

	test("the error of the stream a retry replaced doesn't count for the new one") {
		runTest {
			liveStartStalled(MutableStateFlow(PlayState.ERROR), 20.seconds) shouldBe true
		}
	}
})
