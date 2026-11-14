package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.deviceprofile.DeviceProfileBuilder

// H264 ref frames limits for wide video: at most 12 reference frames from 1200 pixels wide, at most 4 from 1900
private const val H264_WIDE_MIN_WIDTH = 1200
private const val H264_WIDE_MAX_REF_FRAMES = 12
private const val H264_FULL_HD_MIN_WIDTH = 1900
private const val H264_FULL_HD_MAX_REF_FRAMES = 4

internal fun DeviceProfileBuilder.addH264Profiles(mediaTest: MediaCodecCapabilitiesTest, userAVCLevel: Int?) {
	val supportsAVC = mediaTest.supportsAVC()
	val supportsAVCHigh10 = mediaTest.supportsAVCHigh10()
	val avcMainLevel = userAVCLevel ?: mediaTest.getAVCMainLevel()
	val avcHigh10Level = userAVCLevel ?: mediaTest.getAVCHigh10Level()

	addH264SupportedProfiles(supportsAVC, supportsAVCHigh10)
	if (supportsAVC) addH264MainLevelProfile(avcMainLevel)
	if (supportsAVCHigh10) addH264High10LevelProfile(avcHigh10Level)
	addH264RefFramesProfiles()
}

private fun DeviceProfileBuilder.addH264SupportedProfiles(supportsAVC: Boolean, supportsAVCHigh10: Boolean) {
	// H264 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.H264

		conditions {
			when {
				!supportsAVC -> ProfileConditionValue.VIDEO_PROFILE equals "none"
				else -> ProfileConditionValue.VIDEO_PROFILE inCollection listOfNotNull(
					"high",
					"main",
					"baseline",
					"constrained baseline",
					if (supportsAVCHigh10) "high 10" else null
				)
			}
		}
	}
}

private fun DeviceProfileBuilder.addH264MainLevelProfile(avcMainLevel: Int) {
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.H264

		conditions {
			ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals avcMainLevel
		}

		applyConditions {
			ProfileConditionValue.VIDEO_PROFILE inCollection listOf(
				"high",
				"main",
				"baseline",
				"constrained baseline"
			)
		}
	}
}

private fun DeviceProfileBuilder.addH264High10LevelProfile(avcHigh10Level: Int) {
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.H264

		conditions {
			ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals avcHigh10Level
		}

		applyConditions {
			ProfileConditionValue.VIDEO_PROFILE equals "high 10"
		}
	}
}

private fun DeviceProfileBuilder.addH264RefFramesProfiles() {
	// H264 ref frames profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.H264

		conditions {
			ProfileConditionValue.REF_FRAMES lowerThanOrEquals H264_WIDE_MAX_REF_FRAMES
		}

		applyConditions {
			ProfileConditionValue.WIDTH greaterThanOrEquals H264_WIDE_MIN_WIDTH
		}
	}

	// H264 ref frames profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.H264

		conditions {
			ProfileConditionValue.REF_FRAMES lowerThanOrEquals H264_FULL_HD_MAX_REF_FRAMES
		}

		applyConditions {
			ProfileConditionValue.WIDTH greaterThanOrEquals H264_FULL_HD_MIN_WIDTH
		}
	}
}
