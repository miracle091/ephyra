package org.jellyfin.androidtv.util.profile

import androidx.media3.common.MimeTypes
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.deviceprofile.DeviceProfileBuilder

private const val STEREO_CHANNELS = 2
private const val SURROUND_CHANNELS = 8

/**
 * Adds the codec profiles that describe what the video decoders and the player can play, in the order the server evaluates them.
 */
internal fun DeviceProfileBuilder.addVideoCodecProfiles(mediaTest: MediaCodecCapabilitiesTest, options: DeviceProfileOptions) {
	addH264Profiles(mediaTest, options.codecLevels.userAVCLevel)
	addHevcProfiles(mediaTest, options.codecLevels.userHEVCLevel)
	addAv1AndVc1Profiles(mediaTest)
	addMaxResolutionProfiles(mediaTest)
	addRotationProfile()
	addHdrRangeProfiles(mediaTest, options.hdr)
	addAudioProfiles(options.audio.downMixAudio)
}

private fun DeviceProfileBuilder.addHevcProfiles(mediaTest: MediaCodecCapabilitiesTest, userHEVCLevel: Int?) {
	val supportsHevc = mediaTest.supportsHevc()
	val supportsHevcMain10 = mediaTest.supportsHevcMain10()
	val hevcMainLevel = userHEVCLevel ?: mediaTest.getHevcMainLevel()
	val hevcMain10Level = userHEVCLevel ?: mediaTest.getHevcMain10Level()

	// HEVC profiles
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.HEVC

		conditions {
			when {
				!supportsHevc -> ProfileConditionValue.VIDEO_PROFILE equals "none"
				else -> ProfileConditionValue.VIDEO_PROFILE inCollection listOfNotNull(
					"main",
					if (supportsHevcMain10) "main 10" else null
				)
			}
		}
	}
	if (supportsHevc) {
		codecProfile {
			type = CodecType.VIDEO
			codec = Codec.Video.HEVC

			conditions {
				ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals hevcMainLevel
			}

			applyConditions {
				ProfileConditionValue.VIDEO_PROFILE equals "main"
			}
		}
	}
	if (supportsHevcMain10) {
		codecProfile {
			type = CodecType.VIDEO
			codec = Codec.Video.HEVC

			conditions {
				ProfileConditionValue.VIDEO_LEVEL lowerThanOrEquals hevcMain10Level
			}

			applyConditions {
				ProfileConditionValue.VIDEO_PROFILE equals "main 10"
			}
		}
	}
}

private fun DeviceProfileBuilder.addAv1AndVc1Profiles(mediaTest: MediaCodecCapabilitiesTest) {
	val supportsAV1 = mediaTest.supportsAV1()
	val supportsAV1Main10 = mediaTest.supportsAV1Main10()
	val supportsVC1 = mediaTest.supportsVc1()

	// AV1 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.AV1

		conditions {
			when {
				!supportsAV1 -> ProfileConditionValue.VIDEO_PROFILE equals "none"
				!supportsAV1Main10 -> ProfileConditionValue.VIDEO_PROFILE notEquals "main 10"
				else -> ProfileConditionValue.VIDEO_PROFILE notEquals "none"
			}
		}
	}

	// VC1 profile
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.VC1

		conditions {
			when {
				!supportsVC1 -> ProfileConditionValue.VIDEO_PROFILE equals "none"
				else -> ProfileConditionValue.VIDEO_PROFILE notEquals "none"
			}
		}
	}
}

// Get max resolutions for common codecs
private fun DeviceProfileBuilder.addMaxResolutionProfiles(mediaTest: MediaCodecCapabilitiesTest) {
	val maxResolutionAVC = mediaTest.getMaxResolution(MimeTypes.VIDEO_H264)
	val maxResolutionHevc = mediaTest.getMaxResolution(MimeTypes.VIDEO_H265)
	val maxResolutionAV1 = mediaTest.getMaxResolution(MimeTypes.VIDEO_AV1)
	val av1SoftwareOnly = !mediaTest.supportsHardwareAV1()
	val maxResolutionVC1 = mediaTest.getMaxResolution(MimeTypes.VIDEO_VC1)

	// AVC
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.H264

		conditions {
			ProfileConditionValue.WIDTH lowerThanOrEquals maxResolutionAVC.width
			ProfileConditionValue.HEIGHT lowerThanOrEquals maxResolutionAVC.height
		}
	}

	// HEVC
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.HEVC

		conditions {
			ProfileConditionValue.WIDTH lowerThanOrEquals maxResolutionHevc.width
			ProfileConditionValue.HEIGHT lowerThanOrEquals maxResolutionHevc.height
		}
	}

	// AV1
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.AV1

		conditions {
			ProfileConditionValue.WIDTH lowerThanOrEquals SoftwareAv1Limit.width(maxResolutionAV1.width, av1SoftwareOnly)
			ProfileConditionValue.HEIGHT lowerThanOrEquals SoftwareAv1Limit.height(maxResolutionAV1.height, av1SoftwareOnly)
		}
	}

	// VC1
	codecProfile {
		type = CodecType.VIDEO
		codec = Codec.Video.VC1

		conditions {
			ProfileConditionValue.WIDTH lowerThanOrEquals maxResolutionVC1.width
			ProfileConditionValue.HEIGHT lowerThanOrEquals maxResolutionVC1.height
		}
	}
}

// Velella: rotated video. MediaCodecVideoRenderer hands the rotation to the decoder (MediaFormat.KEY_ROTATION), which needs the
// extractor to read it. The MP4 family extractor does (display matrix in tkhd). The Matroska extractor reads ProjectionPoseRoll, but
// whether its sign matches what phones and ffmpeg write is unverified, and the other containers carry no rotation Media3 understands.
// For those the server handles any rotation (remux or transcode). A missing rotation counts as 0 (isRequired = false).
// The SDK accepts only servers 12+, which know this condition.
private fun DeviceProfileBuilder.addRotationProfile() {
	codecProfile {
		type = CodecType.VIDEO
		container = rotationUnawareContainers.joinToString(",")

		conditions {
			equals(ProfileConditionValue.VIDEO_ROTATION, 0, false)
		}
	}
}

private fun DeviceProfileBuilder.addAudioProfiles(downMixAudio: Boolean) {
	// Audio channel profile
	codecProfile {
		type = CodecType.VIDEO_AUDIO

		conditions {
			ProfileConditionValue.AUDIO_CHANNELS lowerThanOrEquals if (downMixAudio) STEREO_CHANNELS else SURROUND_CHANNELS
		}
	}

	// Velella: Media3's MPEG-TS extractor has no reader for TrueHD (Blu-ray stream type 0x83), so the track would be missing:
	// a condition no stream meets makes the server remux or convert TrueHD in TS
	codecProfile {
		type = CodecType.VIDEO_AUDIO
		codec = Codec.Audio.TRUEHD
		container = listOf(Codec.Container.TS, Codec.Container.MPEGTS, Codec.Container.M2TS).joinToString(",")

		conditions {
			ProfileConditionValue.AUDIO_CHANNELS lowerThanOrEquals 0
		}
	}
}
