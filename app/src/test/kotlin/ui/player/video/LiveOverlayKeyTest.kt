package org.jellyfin.androidtv.ui.player.video

import android.view.KeyEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.sdk.model.api.BaseItemDto

class LiveOverlayKeyTest : FunSpec({
	fun live(onChannels: () -> Unit = {}) = LiveControls(
		info = LiveChannelInfo(mockk<BaseItemDto>(), null, null),
		playInfo = null,
		actions = LiveTvActions(onGuide = {}, onRecord = {}, onChannels = onChannels, onSwitch = {}),
	)

	test("down with the controls hidden opens the channel bar of a live channel") {
		var opened = false
		liveOverlayKey(KeyEvent.KEYCODE_DPAD_DOWN, live { opened = true }, channelBarOpen = false, controlsVisible = false) shouldBe true
		opened shouldBe true
	}

	test("down moves the focus as usual with the controls on screen, or for anything but a live channel") {
		liveOverlayKey(KeyEvent.KEYCODE_DPAD_DOWN, live(), channelBarOpen = false, controlsVisible = true) shouldBe null
		liveOverlayKey(KeyEvent.KEYCODE_DPAD_DOWN, null, channelBarOpen = false, controlsVisible = false) shouldBe null
	}

	test("the open channel bar keeps every key from the player") {
		liveOverlayKey(KeyEvent.KEYCODE_DPAD_CENTER, live(), channelBarOpen = true, controlsVisible = false) shouldBe false
	}
})
