package org.jellyfin.androidtv.ui.player.video

import android.view.KeyEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.ui.playback.overlay.SkipButtonState

class SkipSegmentTest : FunSpec({
	test("on a shown button centre skips and back closes it") {
		skipKeyAction(SkipButtonState.SHOWN, KeyEvent.KEYCODE_DPAD_CENTER) shouldBe SkipKeyAction.SKIP
		skipKeyAction(SkipButtonState.SHOWN, KeyEvent.KEYCODE_ENTER) shouldBe SkipKeyAction.SKIP
		skipKeyAction(SkipButtonState.SHOWN, KeyEvent.KEYCODE_BACK) shouldBe SkipKeyAction.DISMISS
		skipKeyAction(SkipButtonState.SHOWN, KeyEvent.KEYCODE_DPAD_LEFT) shouldBe null
	}

	test("a button that hid by itself comes back on centre, up or down, not on left and right") {
		skipKeyAction(SkipButtonState.RECALLABLE, KeyEvent.KEYCODE_DPAD_CENTER) shouldBe SkipKeyAction.RECALL
		skipKeyAction(SkipButtonState.RECALLABLE, KeyEvent.KEYCODE_DPAD_UP) shouldBe SkipKeyAction.RECALL
		skipKeyAction(SkipButtonState.RECALLABLE, KeyEvent.KEYCODE_DPAD_DOWN) shouldBe SkipKeyAction.RECALL
		skipKeyAction(SkipButtonState.RECALLABLE, KeyEvent.KEYCODE_DPAD_RIGHT) shouldBe null
		skipKeyAction(SkipButtonState.RECALLABLE, KeyEvent.KEYCODE_BACK) shouldBe null
	}

	test("without a button no key is taken") {
		skipKeyAction(SkipButtonState.NONE, KeyEvent.KEYCODE_DPAD_CENTER) shouldBe null
		skipKeyAction(SkipButtonState.NONE, KeyEvent.KEYCODE_BACK) shouldBe null
	}
})
