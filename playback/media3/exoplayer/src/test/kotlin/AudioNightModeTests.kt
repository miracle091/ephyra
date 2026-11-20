package org.jellyfin.playback.media3.exoplayer

import android.media.audiofx.AudioEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify

class AudioNightModeTests : FunSpec({
	test("the device's whole output mix (session 0) is never processed") {
		val sessions = mutableListOf<Int>()
		val nightMode = AudioNightMode { sessions += it; mockk(relaxed = true) }

		nightMode.apply(0)

		sessions.shouldBeEmpty()
	}

	test("a new audio session replaces the effect of the previous one") {
		val effects = mutableListOf<AudioEffect>()
		val nightMode = AudioNightMode { mockk<AudioEffect>(relaxed = true).also(effects::add) }

		nightMode.apply(5)
		nightMode.apply(6)

		effects.size shouldBe 2
		verify(exactly = 1) { effects[0].release() }
		verify(exactly = 0) { effects[1].release() }
	}

	test("applying again to the same session keeps the running effect") {
		val effects = mutableListOf<AudioEffect>()
		val nightMode = AudioNightMode { mockk<AudioEffect>(relaxed = true).also(effects::add) }

		nightMode.apply(5)
		nightMode.apply(5)

		effects.size shouldBe 1
		verify(exactly = 0) { effects[0].release() }
	}

	test("release frees the effect once") {
		val effect = mockk<AudioEffect>(relaxed = true)
		val nightMode = AudioNightMode { effect }

		nightMode.apply(5)
		nightMode.release()
		nightMode.release()

		verify(exactly = 1) { effect.release() }
	}

	test("a session the platform refuses leaves no effect behind") {
		val nightMode = AudioNightMode { error("not supported") }

		nightMode.apply(5)
		nightMode.release()
	}
})
