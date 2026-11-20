package org.jellyfin.androidtv.ui.player.video

import androidx.compose.ui.unit.IntSize
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.ZoomMode

class VideoFrameTest : FunSpec({
	val screen = IntSize(1920, 1080)
	val scope = 2.39f
	val academy = 4f / 3f

	test("fit letterboxes a wide film") {
		videoFrameSize(screen, scope, ZoomMode.FIT) shouldBe IntSize(1920, 803)
	}

	test("fit pillarboxes a 4:3 video") {
		videoFrameSize(screen, academy, ZoomMode.FIT) shouldBe IntSize(1440, 1080)
	}

	test("auto crop fills the height with a wide film and cuts the sides") {
		videoFrameSize(screen, scope, ZoomMode.AUTO_CROP) shouldBe IntSize(2581, 1080)
	}

	test("auto crop fills the width with a 4:3 video and cuts top and bottom") {
		videoFrameSize(screen, academy, ZoomMode.AUTO_CROP) shouldBe IntSize(1920, 1440)
	}

	test("stretch fills the screen whatever the shape") {
		videoFrameSize(screen, scope, ZoomMode.STRETCH) shouldBe screen
	}

	test("a 16:9 video fills a 16:9 screen in every mode") {
		ZoomMode.entries.forEach { mode -> videoFrameSize(screen, 16f / 9f, mode) shouldBe screen }
	}

	test("an unknown shape fills the container") {
		videoFrameSize(screen, Float.NaN, ZoomMode.FIT) shouldBe screen
	}
})
