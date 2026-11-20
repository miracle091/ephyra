package org.jellyfin.androidtv.ui.player.video

import android.view.KeyEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VideoPlayerKeyActionTest : FunSpec({
	test("media keys and the shoulder buttons work with the controls shown or hidden") {
		for (visible in listOf(true, false)) {
			videoPlayerKeyAction(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, visible) shouldBe VideoPlayerKeyAction.PLAY_PAUSE
			videoPlayerKeyAction(KeyEvent.KEYCODE_MEDIA_PLAY, visible) shouldBe VideoPlayerKeyAction.PLAY
			videoPlayerKeyAction(KeyEvent.KEYCODE_MEDIA_PAUSE, visible) shouldBe VideoPlayerKeyAction.PAUSE
			videoPlayerKeyAction(KeyEvent.KEYCODE_BUTTON_R1, visible) shouldBe VideoPlayerKeyAction.FAST_FORWARD
			videoPlayerKeyAction(KeyEvent.KEYCODE_BUTTON_R2, visible) shouldBe VideoPlayerKeyAction.FAST_FORWARD
			videoPlayerKeyAction(KeyEvent.KEYCODE_BUTTON_L1, visible) shouldBe VideoPlayerKeyAction.REWIND
			videoPlayerKeyAction(KeyEvent.KEYCODE_MEDIA_REWIND, visible) shouldBe VideoPlayerKeyAction.REWIND
			videoPlayerKeyAction(KeyEvent.KEYCODE_MEDIA_STOP, visible) shouldBe VideoPlayerKeyAction.STOP
			videoPlayerKeyAction(KeyEvent.KEYCODE_CAPTIONS, visible) shouldBe VideoPlayerKeyAction.SUBTITLES_MENU
			videoPlayerKeyAction(KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK, visible) shouldBe VideoPlayerKeyAction.AUDIO_MENU
		}
	}

	test("centre plays or pauses only while the controls are hidden") {
		videoPlayerKeyAction(KeyEvent.KEYCODE_DPAD_CENTER, controlsVisible = false) shouldBe VideoPlayerKeyAction.PLAY_PAUSE
		videoPlayerKeyAction(KeyEvent.KEYCODE_ENTER, controlsVisible = false) shouldBe VideoPlayerKeyAction.PLAY_PAUSE
		videoPlayerKeyAction(KeyEvent.KEYCODE_DPAD_CENTER, controlsVisible = true) shouldBe null
	}

	test("left and right seek quickly while the controls are hidden") {
		videoPlayerKeyAction(KeyEvent.KEYCODE_DPAD_RIGHT, controlsVisible = false) shouldBe VideoPlayerKeyAction.QUICK_SEEK_FORWARD
		videoPlayerKeyAction(KeyEvent.KEYCODE_DPAD_LEFT, controlsVisible = false) shouldBe VideoPlayerKeyAction.QUICK_SEEK_REWIND
	}

	test("arrows and back are left to the focus and the controls") {
		videoPlayerKeyAction(KeyEvent.KEYCODE_DPAD_LEFT, controlsVisible = true) shouldBe null
		videoPlayerKeyAction(KeyEvent.KEYCODE_DPAD_RIGHT, controlsVisible = true) shouldBe null
		videoPlayerKeyAction(KeyEvent.KEYCODE_DPAD_UP, controlsVisible = false) shouldBe null
		videoPlayerKeyAction(KeyEvent.KEYCODE_DPAD_UP, controlsVisible = true) shouldBe null
		videoPlayerKeyAction(KeyEvent.KEYCODE_BACK, controlsVisible = true) shouldBe null
	}
})
