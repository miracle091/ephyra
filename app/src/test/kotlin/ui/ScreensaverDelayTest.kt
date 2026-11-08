package org.jellyfin.androidtv.ui

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes

class ScreensaverDelayTest : FunSpec({
	test("a paused video gets the long pause screen after 2 minutes") {
		screensaverDelay(timeout = 5.minutes, videoPaused = true) shouldBe 2.minutes
	}

	test("otherwise the screensaver waits for its own timeout") {
		screensaverDelay(timeout = 5.minutes, videoPaused = false) shouldBe 5.minutes
	}
})
