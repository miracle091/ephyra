package org.jellyfin.androidtv.util.profile

import androidx.media3.common.MimeTypes

// Capability checks derived from the codec queries of [MediaCodecCapabilitiesTest]

internal fun MediaCodecCapabilitiesTest.supportsAV1Main10(): Boolean = av1.supportsAv1Main10()

internal fun MediaCodecCapabilitiesTest.supportsAV1DolbyVision(): Boolean = av1.supportsAv1DolbyVision()

internal fun MediaCodecCapabilitiesTest.supportsAV1HDR10(): Boolean = av1.supportsAv1HDR10()

internal fun MediaCodecCapabilitiesTest.supportsAV1HDR10Plus(): Boolean = av1.supportsAv1HDR10Plus()

internal fun MediaCodecCapabilitiesTest.supportsAVCHigh10(): Boolean = avc.supportsAvcHigh10()

internal fun MediaCodecCapabilitiesTest.getAVCHigh10Level(): Int = avc.getHigh10Level()

internal fun MediaCodecCapabilitiesTest.supportsVc1(): Boolean = codecQuery.hasCodecForMime(MimeTypes.VIDEO_VC1)
