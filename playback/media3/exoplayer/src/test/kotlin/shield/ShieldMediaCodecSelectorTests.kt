package org.jellyfin.playback.media3.exoplayer.shield

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

@OptIn(UnstableApi::class)
private fun codec(name: String, mimeType: String, hardware: Boolean) = MediaCodecInfo.newInstance(
	name,
	mimeType,
	mimeType,
	null,
	hardware,
	!hardware,
	hardware,
	false,
	false,
)

@OptIn(UnstableApi::class)
private fun selectorReturning(vararg codecs: MediaCodecInfo): ShieldMediaCodecSelector {
	val delegate = mockk<MediaCodecSelector> {
		every { getDecoderInfos(any(), any(), any()) } returns codecs.toList()
	}
	return ShieldMediaCodecSelector(delegate)
}

@OptIn(UnstableApi::class)
private fun ShieldMediaCodecSelector.names(mimeType: String) = getDecoderInfos(mimeType, false, false).map { it.name }

@OptIn(UnstableApi::class)
class ShieldMediaCodecSelectorTests : FunSpec({
	test("HEVC keeps only hardware decoders") {
		val selector = selectorReturning(
			codec("c2.android.hevc.decoder", MimeTypes.VIDEO_H265, hardware = false),
			codec("OMX.Nvidia.h265.decode", MimeTypes.VIDEO_H265, hardware = true),
		)

		selector.names(MimeTypes.VIDEO_H265) shouldBe listOf("OMX.Nvidia.h265.decode")
	}

	test("Hardware-only formats keep every hardware decoder in order, so decoder fallback still works") {
		val selector = selectorReturning(
			codec("OMX.Nvidia.h264.decode", MimeTypes.VIDEO_H264, hardware = true),
			codec("c2.android.avc.decoder", MimeTypes.VIDEO_H264, hardware = false),
			codec("c2.nvidia.avc.decoder", MimeTypes.VIDEO_H264, hardware = true),
		)

		selector.names(MimeTypes.VIDEO_H264) shouldBe listOf("OMX.Nvidia.h264.decode", "c2.nvidia.avc.decoder")
	}

	test("Dolby Vision keeps only hardware decoders") {
		val selector = selectorReturning(
			codec("OMX.Nvidia.DOVI.decode", MimeTypes.VIDEO_DOLBY_VISION, hardware = true),
			codec("c2.example.dolby-vision.decoder", MimeTypes.VIDEO_DOLBY_VISION, hardware = false),
		)

		selector.names(MimeTypes.VIDEO_DOLBY_VISION) shouldBe listOf("OMX.Nvidia.DOVI.decode")
	}

	test("A hardware-only format without a hardware decoder keeps the software one instead of no decoder") {
		val selector = selectorReturning(codec("c2.android.hevc.decoder", MimeTypes.VIDEO_H265, hardware = false))

		selector.names(MimeTypes.VIDEO_H265) shouldBe listOf("c2.android.hevc.decoder")
	}

	test("Secure and tunneling requests are passed on to the delegate") {
		val delegate = mockk<MediaCodecSelector> {
			every { getDecoderInfos(any(), any(), any()) } returns emptyList()
		}

		ShieldMediaCodecSelector(delegate).getDecoderInfos(MimeTypes.VIDEO_VP9, true, true)

		verify { delegate.getDecoderInfos(MimeTypes.VIDEO_VP9, true, true) }
	}

	test("A format without a hardware decoder keeps the software one") {
		val selector = selectorReturning(codec("c2.android.av1.decoder", MimeTypes.VIDEO_AV1, hardware = false))

		selector.names(MimeTypes.VIDEO_AV1) shouldBe listOf("c2.android.av1.decoder")
	}

	test("Other video formats put hardware decoders before software ones") {
		val selector = selectorReturning(
			codec("c2.android.mpeg2.decoder", MimeTypes.VIDEO_MPEG2, hardware = false),
			codec("OMX.Nvidia.mpeg2v.decode", MimeTypes.VIDEO_MPEG2, hardware = true),
		)

		selector.names(MimeTypes.VIDEO_MPEG2) shouldBe listOf("OMX.Nvidia.mpeg2v.decode", "c2.android.mpeg2.decoder")
	}

	test("Audio decoders are left unchanged") {
		val selector = selectorReturning(
			codec("c2.android.aac.decoder", MimeTypes.AUDIO_AAC, hardware = false),
			codec("OMX.vendor.aac.decoder", MimeTypes.AUDIO_AAC, hardware = true),
		)

		selector.names(MimeTypes.AUDIO_AAC) shouldBe listOf("c2.android.aac.decoder", "OMX.vendor.aac.decoder")
	}
})
