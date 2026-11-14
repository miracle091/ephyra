package org.jellyfin.androidtv.util.profile

import org.jellyfin.sdk.model.api.VideoRangeType

/**
 * The user and device settings that shape the device profile, grouped by topic.
 */
data class DeviceProfileOptions(
	val maxBitrate: Int,
	val audio: AudioProfileOptions,
	val subtitles: SubtitleProfileOptions,
	val codecLevels: CodecLevelOverrides,
	val hdr: HdrProfileOptions,
)

data class AudioProfileOptions(
	val isAC3PrefEnabled: Boolean,
	val isEAC3PrefEnabled: Boolean,
	val isDTSPrefEnabled: Boolean,
	val isTrueHDPrefEnabled: Boolean,
	val downMixAudio: Boolean,
)

data class SubtitleProfileOptions(
	val assDirectPlay: Boolean,
	val pgsDirectPlay: Boolean,
)

/** User overrides of the AVC and HEVC levels, null to use the level the decoder reports. */
data class CodecLevelOverrides(
	val userAVCLevel: Int?,
	val userHEVCLevel: Int?,
)

data class HdrProfileOptions(
	val forceEnabledHdr: Set<VideoRangeType>,
	val forceDisabledHdr: Set<VideoRangeType>,
	val displaySupportsDolbyVision: Boolean = true,
	val unreportedDoviProfile7Support: Boolean = KnownDefects.unreportedDoviProfile7Support,
)
