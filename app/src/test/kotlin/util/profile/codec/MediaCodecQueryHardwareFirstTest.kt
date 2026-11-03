package org.jellyfin.androidtv.util.profile.codec

import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import android.os.Build
import androidx.media3.common.MimeTypes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.jellyfin.androidtv.util.AndroidVersion

private const val HEVC_MAIN = 1
private const val HARDWARE_LEVEL = 0x10000
private const val SOFTWARE_LEVEL = 0x40000

private fun decoder(mime: String, softwareOnly: Boolean, level: Int): MediaCodecInfo {
	val profileLevel = mockk<CodecProfileLevel>()
	profileLevel.profile = HEVC_MAIN
	profileLevel.level = level
	val capabilities = mockk<MediaCodecInfo.CodecCapabilities>()
	capabilities.profileLevels = arrayOf(profileLevel)

	return mockk {
		every { isEncoder } returns false
		every { isSoftwareOnly } returns softwareOnly
		every { supportedTypes } returns arrayOf(mime)
		every { getCapabilitiesForType(mime) } returns capabilities
	}
}

private fun query(softwareCodecsEnabled: Boolean, vararg decoders: MediaCodecInfo): MediaCodecQuery {
	val list = mockk<MediaCodecList> {
		every { codecInfos } returns arrayOf(*decoders)
	}
	return MediaCodecQuery(list, softwareCodecsEnabled)
}

class MediaCodecQueryHardwareFirstTest : FunSpec({
	beforeEach {
		mockkObject(AndroidVersion)
		every { AndroidVersion.sdkInt } returns Build.VERSION_CODES.R
	}

	afterEach {
		unmockkObject(AndroidVersion)
	}

	test("HEVC limits come from the hardware decoder even with software codecs enabled") {
		val query = query(
			softwareCodecsEnabled = true,
			decoder(MimeTypes.VIDEO_H265, softwareOnly = true, level = SOFTWARE_LEVEL),
			decoder(MimeTypes.VIDEO_H265, softwareOnly = false, level = HARDWARE_LEVEL),
		)

		query.getDecoderLevel(MimeTypes.VIDEO_H265, HEVC_MAIN) shouldBe HARDWARE_LEVEL
	}

	test("HEVC keeps the software decoder when it is the only one and software codecs are enabled") {
		val query = query(softwareCodecsEnabled = true, decoder(MimeTypes.VIDEO_H265, softwareOnly = true, level = SOFTWARE_LEVEL))

		query.getDecoderLevel(MimeTypes.VIDEO_H265, HEVC_MAIN) shouldBe SOFTWARE_LEVEL
	}

	test("Software decoders are ignored when software codecs are disabled") {
		val query = query(softwareCodecsEnabled = false, decoder(MimeTypes.VIDEO_AV1, softwareOnly = true, level = SOFTWARE_LEVEL))

		query.hasCodecForMime(MimeTypes.VIDEO_AV1) shouldBe false
	}

	test("AV1 software decoders count when software codecs are enabled") {
		val query = query(
			softwareCodecsEnabled = true,
			decoder(MimeTypes.VIDEO_AV1, softwareOnly = true, level = SOFTWARE_LEVEL),
			decoder(MimeTypes.VIDEO_AV1, softwareOnly = false, level = HARDWARE_LEVEL),
		)

		query.getDecoderLevel(MimeTypes.VIDEO_AV1, HEVC_MAIN) shouldBe SOFTWARE_LEVEL
	}

	test("The hardware-only check ignores the case of the MIME type") {
		val query = query(
			softwareCodecsEnabled = true,
			decoder("video/HEVC", softwareOnly = true, level = SOFTWARE_LEVEL),
			decoder("video/HEVC", softwareOnly = false, level = HARDWARE_LEVEL),
		)

		query.getDecoderLevel("video/HEVC", HEVC_MAIN) shouldBe HARDWARE_LEVEL
	}
})
