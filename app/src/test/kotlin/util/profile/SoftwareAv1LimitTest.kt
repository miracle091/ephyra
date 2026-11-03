package org.jellyfin.androidtv.util.profile

import android.util.Size
import androidx.media3.common.MimeTypes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.sdk.model.api.ProfileConditionValue

private fun av1Limits(hardwareAv1: Boolean): Pair<String?, String?> {
	val av1Max = mockk<Size> {
		every { width } returns 3840
		every { height } returns 2160
	}
	val mediaTest = mockk<MediaCodecCapabilitiesTest>(relaxed = true) {
		every { supportsAV1() } returns true
		every { supportsHardwareAV1() } returns hardwareAv1
		every { getMaxResolution(MimeTypes.VIDEO_AV1) } returns av1Max
	}

	val profile = createDeviceProfile(
		mediaTest = mediaTest,
		maxBitrate = 100_000_000,
		isAC3PrefEnabled = false,
		isEAC3PrefEnabled = false,
		isDTSPrefEnabled = false,
		isTrueHDPrefEnabled = false,
		downMixAudio = false,
		assDirectPlay = false,
		pgsDirectPlay = false,
		userAVCLevel = null,
		userHEVCLevel = null,
		forceEnabledHdr = emptySet(),
		forceDisabledHdr = emptySet(),
	)

	val conditions = profile.codecProfiles.orEmpty().filter { it.codec == Codec.Video.AV1 }.flatMap { it.conditions.orEmpty() }
	return conditions.single { it.property == ProfileConditionValue.WIDTH }.value to
		conditions.single { it.property == ProfileConditionValue.HEIGHT }.value
}

class SoftwareAv1LimitTest : FunSpec({
	test("Software-only AV1 is capped at 1080p") {
		av1Limits(hardwareAv1 = false) shouldBe ("1920" to "1080")
	}

	test("Hardware AV1 keeps the decoder's own limits") {
		av1Limits(hardwareAv1 = true) shouldBe ("3840" to "2160")
	}
})
