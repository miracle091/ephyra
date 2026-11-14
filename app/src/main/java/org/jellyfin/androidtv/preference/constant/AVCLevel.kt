package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/**
 * User-defined AVC (H.264) codec level override.
 * Values correspond to MediaCodecInfo.CodecProfileLevel constants.
 */
enum class AVCLevel(
	override val nameRes: Int,
	val level: Int?,
) : PreferenceEnum {
	AUTO(R.string.auto, null),

	LEVEL_6_2(R.string.codec_level_6_2, level = 62),
	LEVEL_6_1(R.string.codec_level_6_1, level = 61),
	LEVEL_6_0(R.string.codec_level_6_0, level = 60),
	LEVEL_5_2(R.string.codec_level_5_2, level = 52),
	LEVEL_5_1(R.string.codec_level_5_1, level = 51),
	LEVEL_5_0(R.string.codec_level_5_0, level = 50),
	LEVEL_4_2(R.string.codec_level_4_2, level = 42),
	LEVEL_4_1(R.string.codec_level_4_1, level = 41),
	LEVEL_4_0(R.string.codec_level_4_0, level = 40),
}
