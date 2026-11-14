package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/**
 * User-defined HEVC (H.265) codec level override.
 * Values correspond to MediaCodecInfo.CodecProfileLevel constants.
 */
enum class HEVCLevel(
	override val nameRes: Int,
	val level: Int?,
) : PreferenceEnum {
	AUTO(R.string.auto, null),

	LEVEL_6_2(R.string.codec_level_6_2, level = 186),
	LEVEL_6_1(R.string.codec_level_6_1, level = 183),
	LEVEL_6_0(R.string.codec_level_6_0, level = 180),
	LEVEL_5_2(R.string.codec_level_5_2, level = 156),
	LEVEL_5_1(R.string.codec_level_5_1, level = 153),
	LEVEL_5_0(R.string.codec_level_5_0, level = 150),
	LEVEL_4_1(R.string.codec_level_4_1, level = 123),
	LEVEL_4_0(R.string.codec_level_4_0, level = 120),
}
