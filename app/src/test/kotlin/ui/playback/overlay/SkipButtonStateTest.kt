package org.jellyfin.androidtv.ui.playback.overlay

import android.view.KeyEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class SkipButtonStateTest : FunSpec({
	val end = 90.seconds

	test("the button shows inside a segment while the player's controls are closed") {
		skipButtonState(enabled = true, autoHidden = false, target = end, position = 30.seconds) shouldBe SkipButtonState.SHOWN
	}

	test("after hiding by itself the button can be called back while the segment lasts") {
		skipButtonState(enabled = true, autoHidden = true, target = end, position = 30.seconds) shouldBe SkipButtonState.RECALLABLE
	}

	test("near or past the segment's end there is nothing to skip") {
		skipButtonState(enabled = true, autoHidden = false, target = end, position = 89.5.seconds) shouldBe SkipButtonState.NONE
		skipButtonState(enabled = true, autoHidden = true, target = end, position = 95.seconds) shouldBe SkipButtonState.NONE
	}

	test("no segment, or the controls open, gives no button") {
		skipButtonState(enabled = true, autoHidden = false, target = null, position = 30.seconds) shouldBe SkipButtonState.NONE
		skipButtonState(enabled = false, autoHidden = false, target = end, position = 30.seconds) shouldBe SkipButtonState.NONE
		skipButtonState(enabled = false, autoHidden = true, target = end, position = 30.seconds) shouldBe SkipButtonState.NONE
	}

	test("centre, enter, up and down call the button back; left and right keep seeking") {
		isSkipRecallKey(KeyEvent.KEYCODE_DPAD_CENTER) shouldBe true
		isSkipRecallKey(KeyEvent.KEYCODE_ENTER) shouldBe true
		isSkipRecallKey(KeyEvent.KEYCODE_DPAD_UP) shouldBe true
		isSkipRecallKey(KeyEvent.KEYCODE_DPAD_DOWN) shouldBe true
		isSkipRecallKey(KeyEvent.KEYCODE_DPAD_LEFT) shouldBe false
		isSkipRecallKey(KeyEvent.KEYCODE_BACK) shouldBe false
	}

	test("the button hides after the chosen seconds, or never when the choice is until the segment ends") {
		skipAutoHideDelay(15) shouldBe 15.seconds
		skipAutoHideDelay(5) shouldBe 5.seconds
		skipAutoHideDelay(0) shouldBe null
		skipAutoHideDelay(-1) shouldBe null
	}

	test("the choices include the 15 second default and the until-segment-end option") {
		SkipAutoHideChoices.contains(15) shouldBe true
		SkipAutoHideChoices.contains(0) shouldBe true
	}
})
