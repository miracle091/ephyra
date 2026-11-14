package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.api.VideoRangeType
import org.jellyfin.sdk.model.deviceprofile.DeviceProfileBuilder

internal fun DeviceProfileBuilder.addHdrRangeProfiles(mediaTest: MediaCodecCapabilitiesTest, hdr: HdrProfileOptions) {
	val unsupportedRangeTypesAv1 = unsupportedAv1RangeTypes(mediaTest, hdr)
	val unsupportedRangeTypesHevc = unsupportedHevcRangeTypes(mediaTest, hdr)

	// Note: The codec profiles use a workaround to create correct behavior
	// The notEquals condition will always fail the ConditionProcessor test in the server so we use applyConditions to only have the codec
	// profile be active when the media in question uses one of the unsupported range types. The server will then use the value of the
	// notEquals in the StreamBuilder to create a correct transcode pipeline

	// Codecs
	// AV1
	if (unsupportedRangeTypesAv1.isNotEmpty()) codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.AV1

		conditions {
			ProfileConditionValue.VIDEO_RANGE_TYPE notEquals unsupportedRangeTypesAv1.joinToString("|") { it.serialName }
		}

		applyConditions {
			ProfileConditionValue.VIDEO_RANGE_TYPE inCollection unsupportedRangeTypesAv1.map { it.serialName }
		}
	}

	// HEVC
	if (unsupportedRangeTypesHevc.isNotEmpty()) codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.HEVC

		conditions {
			ProfileConditionValue.VIDEO_RANGE_TYPE notEquals unsupportedRangeTypesHevc.joinToString("|") { it.serialName }
		}

		applyConditions {
			ProfileConditionValue.VIDEO_RANGE_TYPE inCollection unsupportedRangeTypesHevc.map { it.serialName }
		}
	}
}

private fun unsupportedAv1RangeTypes(mediaTest: MediaCodecCapabilitiesTest, hdr: HdrProfileOptions): Set<VideoRangeType> {
	val supportsAV1DolbyVision = mediaTest.supportsAV1DolbyVision()
	val supportsAV1HDR10 = mediaTest.supportsAV1HDR10()
	val supportsAV1HDR10Plus = mediaTest.supportsAV1HDR10Plus()

	return buildSet {
		add(VideoRangeType.DOVI_INVALID)

		if (!supportsAV1DolbyVision) {
			add(VideoRangeType.DOVI)
			if (!supportsAV1HDR10) add(VideoRangeType.DOVI_WITH_HDR10)
			if (!supportsAV1HDR10Plus) add(VideoRangeType.DOVI_WITH_HDR10_PLUS)
		}

		if (!supportsAV1HDR10Plus) {
			add(VideoRangeType.HDR10_PLUS)

			if (!supportsAV1HDR10) add(VideoRangeType.HDR10)
		}
	} - hdr.forceEnabledHdr + hdr.forceDisabledHdr
}

private fun unsupportedHevcRangeTypes(mediaTest: MediaCodecCapabilitiesTest, hdr: HdrProfileOptions): Set<VideoRangeType> {
	val supportsHevcDolbyVisionEL = mediaTest.supportsHevcDolbyVisionEL()
	val supportsHevcHDR10 = mediaTest.supportsHevcHDR10()
	val supportsHevcHDR10Plus = mediaTest.supportsHevcHDR10Plus()

	return buildSet {
		add(VideoRangeType.DOVI_INVALID)

		if (!supportsHevcDolbyVisionEL) addHevcDolbyVisionExclusions(mediaTest, hdr)

		if (!supportsHevcHDR10Plus) {
			add(VideoRangeType.HDR10_PLUS)
			if (!supportsHevcHDR10) add(VideoRangeType.HDR10)
		}

		if (KnownDefects.hevcDoviHdr10PlusBug) {
			add(VideoRangeType.DOVI_WITH_HDR10_PLUS)
			add(VideoRangeType.DOVI_WITH_ELHDR10_PLUS)
		}
	} - hdr.forceEnabledHdr + hdr.forceDisabledHdr
}

/** The HEVC range types to exclude when the decoder has no Dolby Vision enhancement layer support. */
private fun MutableSet<VideoRangeType>.addHevcDolbyVisionExclusions(mediaTest: MediaCodecCapabilitiesTest, hdr: HdrProfileOptions) {
	val supportsHevcMain10 = mediaTest.supportsHevcMain10()
	val supportsHevcDolbyVision = mediaTest.supportsHevcDolbyVision()
	val supportsHevcHDR10 = mediaTest.supportsHevcHDR10()
	val supportsHevcHDR10Plus = mediaTest.supportsHevcHDR10Plus()

	val decodesHevcDolbyVisionHdr10 = supportsHevcDolbyVision && supportsHevcMain10 && supportsHevcHDR10
	if (
		!hdr.unreportedDoviProfile7Support ||
		// Ephyra: without a Dolby Vision display the decoder gives SDR; the server strips the RPU and sends the HDR10 layer
		!hdr.displaySupportsDolbyVision ||
		!decodesHevcDolbyVisionHdr10
	) {
		add(VideoRangeType.DOVI_WITH_EL)

		if (!supportsHevcHDR10Plus && !KnownDefects.hevcDoviHdr10PlusBug) {
			add(VideoRangeType.DOVI_WITH_ELHDR10_PLUS)
		}
	}

	if (!supportsHevcDolbyVision) {
		add(VideoRangeType.DOVI)
		if (!supportsHevcHDR10) add(VideoRangeType.DOVI_WITH_HDR10)
		if (!supportsHevcHDR10Plus && !KnownDefects.hevcDoviHdr10PlusBug) add(VideoRangeType.DOVI_WITH_HDR10_PLUS)
	}
}
