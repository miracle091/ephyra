package org.jellyfin.androidtv.util.profile

import android.media.MediaCodecInfo
import android.util.Size
import android.view.Display
import androidx.media3.common.MimeTypes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.preference.constant.HdrFormat
import org.jellyfin.androidtv.util.profile.codec.hasPlatformAudioDecoder
import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.ProfileConditionType
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.api.VideoRangeType

private fun profile(
	dtsEnabled: Boolean = false,
	doviElSupported: Boolean = false,
	displaySupportsDolbyVision: Boolean = true,
	unreportedDoviProfile7Support: Boolean = false,
	forceEnabledHdr: Set<VideoRangeType> = emptySet(),
): DeviceProfile {
	val size = mockk<Size> {
		every { width } returns 3840
		every { height } returns 2160
	}
	val mediaTest = mockk<MediaCodecCapabilitiesTest>(relaxed = true) {
		every { supportsHevc() } returns true
		every { supportsHevcMain10() } returns true
		every { supportsHevcHDR10() } returns true
		every { supportsHevcDolbyVision() } returns true
		every { supportsHevcDolbyVisionEL() } returns doviElSupported
		every { getMaxResolution(any()) } returns size
	}

	return createDeviceProfile(
		mediaTest = mediaTest,
		maxBitrate = 100_000_000,
		isAC3PrefEnabled = false,
		isEAC3PrefEnabled = false,
		isDTSPrefEnabled = dtsEnabled,
		isTrueHDPrefEnabled = false,
		downMixAudio = false,
		assDirectPlay = false,
		pgsDirectPlay = false,
		userAVCLevel = null,
		userHEVCLevel = null,
		forceEnabledHdr = forceEnabledHdr,
		forceDisabledHdr = emptySet(),
		displaySupportsDolbyVision = displaySupportsDolbyVision,
		unreportedDoviProfile7Support = unreportedDoviProfile7Support,
	)
}

private fun DeviceProfile.unsupportedHevcRangeTypes(): List<String> = codecProfiles.orEmpty()
	.filter { it.codec == Codec.Video.HEVC }
	.flatMap { it.applyConditions.orEmpty() }
	.filter { it.property == ProfileConditionValue.VIDEO_RANGE_TYPE }
	.flatMap { it.value.orEmpty().split("|") }

private fun DeviceProfile.directPlayAudioCodecs(): List<String> =
	directPlayProfiles.orEmpty().first { it.type == DlnaProfileType.VIDEO }.audioCodec.orEmpty().split(",")

private fun decoder(mime: String, encoder: Boolean = false, softwareOnly: Boolean = false): MediaCodecInfo = mockk {
	every { isEncoder } returns encoder
	every { isSoftwareOnly } returns softwareOnly
	every { supportedTypes } returns arrayOf(mime)
}

class EphyraDeviceProfileTest : FunSpec({
	context("video rotation") {
		test("containers whose rotation the player ignores require rotation 0, a missing rotation passes") {
			val rotation = profile().codecProfiles.orEmpty()
				.filter { it.type == CodecType.VIDEO && it.codec == null }
				.flatMap { it.conditions.orEmpty().map { condition -> it to condition } }
				.single { (_, condition) -> condition.property == ProfileConditionValue.VIDEO_ROTATION }

			val (codecProfile, condition) = rotation
			condition.condition shouldBe ProfileConditionType.EQUALS
			condition.value shouldBe "0"
			condition.isRequired shouldBe false
			codecProfile.container.orEmpty().split(",") shouldContain Codec.Container.MKV
			codecProfile.container.orEmpty().split(",") shouldNotContain Codec.Container.MP4
			codecProfile.container.orEmpty().split(",") shouldNotContain Codec.Container.MOV
		}
	}

	context("DTS family") {
		test("dca follows the DTS preference") {
			profile(dtsEnabled = false).directPlayAudioCodecs().apply {
				shouldNotContain(Codec.Audio.DTS)
				shouldNotContain(Codec.Audio.DCA)
			}
			profile(dtsEnabled = true).directPlayAudioCodecs().apply {
				shouldContain(Codec.Audio.DTS)
				shouldContain(Codec.Audio.DCA)
			}
		}
	}

	context("platform audio decoders") {
		test("a hardware decoder counts") {
			hasPlatformAudioDecoder(MimeTypes.AUDIO_E_AC3, arrayOf(decoder(MimeTypes.AUDIO_E_AC3))) shouldBe true
		}

		test("software-only decoders, encoders and other formats do not count") {
			val infos = arrayOf(
				decoder(MimeTypes.AUDIO_E_AC3, softwareOnly = true),
				decoder(MimeTypes.AUDIO_E_AC3, encoder = true),
				decoder(MimeTypes.AUDIO_AC3),
			)
			hasPlatformAudioDecoder(MimeTypes.AUDIO_E_AC3, infos) shouldBe false
		}
	}

	context("Dolby Vision profile 7") {
		val elTypes = listOf(VideoRangeType.DOVI_WITH_EL.serialName, VideoRangeType.DOVI_WITH_ELHDR10_PLUS.serialName)

		test("the unreported support workaround keeps profile 7 direct play on a Dolby Vision display") {
			val unsupported = profile(unreportedDoviProfile7Support = true, displaySupportsDolbyVision = true).unsupportedHevcRangeTypes()
			elTypes.forEach { unsupported shouldNotContain it }
		}

		test("without a Dolby Vision display profile 7 is unsupported, so the server strips the RPU") {
			val unsupported = profile(unreportedDoviProfile7Support = true, displaySupportsDolbyVision = false).unsupportedHevcRangeTypes()
			elTypes.forEach { unsupported shouldContain it }
		}

		test("the profile 7 override still wins on a display without Dolby Vision") {
			val unsupported = profile(
				unreportedDoviProfile7Support = true,
				displaySupportsDolbyVision = false,
				forceEnabledHdr = HdrFormat.DOVI_PROFILE_7.videoRangeTypes,
			).unsupportedHevcRangeTypes()
			elTypes.forEach { unsupported shouldNotContain it }
		}
	}

	context("DOVI_INVALID") {
		test("is unsupported by default") {
			profile().unsupportedHevcRangeTypes() shouldContain VideoRangeType.DOVI_INVALID.serialName
		}

		test("the override entry lets it direct play") {
			profile(forceEnabledHdr = HdrFormat.DOVI_INVALID.videoRangeTypes).unsupportedHevcRangeTypes() shouldNotContain
				VideoRangeType.DOVI_INVALID.serialName
		}
	}

	context("display Dolby Vision") {
		test("reads the Dolby Vision type from the HDR type list") {
			hdrTypesIncludeDolbyVision(intArrayOf(Display.HdrCapabilities.HDR_TYPE_HDR10)) shouldBe false
			hdrTypesIncludeDolbyVision(
				intArrayOf(Display.HdrCapabilities.HDR_TYPE_HDR10, Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION)
			) shouldBe true
			hdrTypesIncludeDolbyVision(null) shouldBe false
		}
	}
})
