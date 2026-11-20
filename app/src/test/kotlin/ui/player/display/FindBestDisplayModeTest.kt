package org.jellyfin.androidtv.ui.player.display

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior

class FindBestDisplayModeTest : FunSpec({
	var nextId = 1
	fun mode(width: Int, height: Int, rate: Float) = DisplayModeInfo(nextId++, DisplaySize(width, height), rate)

	// A 4K TV as the Shield reports it
	val uhd60 = mode(3840, 2160, 60f)
	val uhd5994 = mode(3840, 2160, 59.94f)
	val uhd50 = mode(3840, 2160, 50f)
	val uhd24 = mode(3840, 2160, 24f)
	val uhd23976 = mode(3840, 2160, 23.976f)
	val fhd60 = mode(1920, 1080, 60f)
	val fhd23976 = mode(1920, 1080, 23.976f)
	val sd60 = mode(720, 480, 60f)
	val modes = listOf(uhd60, uhd5994, uhd50, uhd24, uhd23976, fhd60, fhd23976, sd60)

	fun best(video: VideoModeRequest, behavior: RefreshRateSwitchingBehavior = RefreshRateSwitchingBehavior.SCALE_ON_DEVICE) =
		findBestDisplayMode(modes, uhd60, video, behavior)

	test("a 1080p 23.976 fps film keeps the native 4K resolution and gets the exact rate when scaling on the device") {
		best(VideoModeRequest(1920, 1080, 23.976f)) shouldBe uhd23976
	}

	test("scaling on the TV picks the mode closest to the video's resolution") {
		best(VideoModeRequest(1920, 1080, 23.976f), RefreshRateSwitchingBehavior.SCALE_ON_TV) shouldBe fhd23976
	}

	test("25 fps video plays at twice its rate") {
		best(VideoModeRequest(1920, 1080, 25f)) shouldBe uhd50
	}

	test("29.97 fps video plays at twice its rate on the 59.94 Hz mode, not 60 Hz") {
		best(VideoModeRequest(1920, 1080, 29.97f)) shouldBe uhd5994
	}

	test("a mode smaller than the video is never picked") {
		val uhdVideo = VideoModeRequest(3840, 2160, 23.976f)
		findBestDisplayMode(listOf(fhd23976), fhd23976, uhdVideo, RefreshRateSwitchingBehavior.SCALE_ON_TV) shouldBe null
	}

	test("modes below HD are skipped") {
		findBestDisplayMode(listOf(sd60), sd60, VideoModeRequest(640, 360, 30f), RefreshRateSwitchingBehavior.SCALE_ON_TV) shouldBe null
	}

	test("no mode at a fitting rate gives no switch") {
		best(VideoModeRequest(1920, 1080, 15f)) shouldBe null
	}
})
