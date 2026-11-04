package org.jellyfin.androidtv.benchmark

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class DecoderResultTest : FunSpec({
	test("Smooth needs frames, no error, no rebuffering and under 1% dropped") {
		DecoderResult("a", renderedFrames = 1000, droppedFrames = 9).smooth shouldBe true
		DecoderResult("a", renderedFrames = 1000, droppedFrames = 20).smooth shouldBe false
		DecoderResult("a", renderedFrames = 1000, rebuffers = 1).smooth shouldBe false
		DecoderResult("a", renderedFrames = 1000, error = "ERROR_CODE_DECODING_FAILED").smooth shouldBe false
		DecoderResult("a").smooth shouldBe false
	}

	test("Dropped share counts dropped frames among all decoded ones") {
		DecoderResult("a", renderedFrames = 1, droppedFrames = 1).droppedShare * 2 shouldBe 1.0
		DecoderResult("a").droppedShare shouldBe 0.0
	}

	test("The report has a row per clip with decoder, drops and outcome") {
		val report = decoderReport(
			listOf(
				DecoderResult("1080p AVC 10M", "OMX.Nvidia.h264.decode", true, firstFrameMs = 420, renderedFrames = 597, droppedFrames = 3),
				DecoderResult("4K AV1 10bit 100M", "c2.android.av1.decoder", false, error = "ERROR_CODE_DECODING_FAILED"),
			)
		)

		report shouldContain "| 1080p AVC 10M | OMX.Nvidia.h264.decode | yes | 420 ms | 597 | 3 (0.5%) | 0 | smooth |"
		report shouldContain "| 4K AV1 10bit 100M | c2.android.av1.decoder | no | - | 0 | 0 (0.0%) | 0 | error: ERROR_CODE_DECODING_FAILED |"
	}
})
