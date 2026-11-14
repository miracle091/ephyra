package org.jellyfin.androidtv.ui.browsing.composable.inforow

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.getResolutionName
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.VideoRangeType

private fun List<MediaStream>.getDefault(type: MediaStreamType, defaultIndex: Int? = null): MediaStream? {
	if (defaultIndex != null) {
		val byIndex = getOrNull(defaultIndex)
		if (byIndex?.type == type) return byIndex
	}

	return firstOrNull { it.type == type }
}

@Composable
internal fun InfoRowDefaultBadge(text: String) {
	InfoRowItem(
		contentDescription = null,
		colors = InfoRowColors.Default,
	) {
		Text(text)
	}
}

@Composable
private fun InfoRowSubtitleBadges(mediaStreams: List<MediaStream>?) {
	val hasSdhSubtitleStream = mediaStreams?.any { it.type == MediaStreamType.SUBTITLE && it.isHearingImpaired } == true
	val hasCcSubtitleStream = mediaStreams?.any { it.type == MediaStreamType.SUBTITLE && !it.isHearingImpaired } == true

	if (hasSdhSubtitleStream) InfoRowDefaultBadge(stringResource(R.string.indicator_subtitles_hearing_impaired))
	if (hasCcSubtitleStream) InfoRowDefaultBadge(stringResource(R.string.indicator_subtitles))
}

/** The names shown for each video range type, as string resources. A type that is not listed shows no badge. */
private val videoRangeNameResources: Map<VideoRangeType, List<Int>> = mapOf(
	VideoRangeType.SDR to listOf(R.string.sdr),
	VideoRangeType.HDR10 to listOf(R.string.hdr10),
	VideoRangeType.HDR10_PLUS to listOf(R.string.hdr10_plus),
	VideoRangeType.HLG to listOf(R.string.hlg),
	VideoRangeType.DOVI to listOf(R.string.dolby_vision),
	VideoRangeType.DOVI_WITH_HDR10 to listOf(R.string.dolby_vision, R.string.hdr10),
	VideoRangeType.DOVI_WITH_HLG to listOf(R.string.dolby_vision, R.string.hlg),
	VideoRangeType.DOVI_WITH_SDR to listOf(R.string.dolby_vision, R.string.sdr),
	VideoRangeType.DOVI_WITH_EL to listOf(R.string.dolby_vision_with_el),
	VideoRangeType.DOVI_WITH_HDR10_PLUS to listOf(R.string.dolby_vision, R.string.hdr10_plus),
	VideoRangeType.DOVI_WITH_ELHDR10_PLUS to listOf(R.string.dolby_vision_with_el, R.string.hdr10_plus),
	VideoRangeType.DOVI_INVALID to listOf(R.string.dolby_vision),
)

@Composable
private fun InfoRowVideoRange(videoRangeType: VideoRangeType?) {
	val videoRangeNames: Set<String> = videoRangeNameResources[videoRangeType].orEmpty().map { stringResource(it) }.toSet()
	videoRangeNames.forEach { InfoRowDefaultBadge(it) }
}

@Composable
private fun audioCodecName(audioStream: MediaStream?): String? {
	val profile = audioStream?.profile
	val codec = audioStream?.codec?.uppercase()

	return when {
		profile?.contains("Dolby Atmos", ignoreCase = true) == true -> when (codec) {
			"EAC3" -> stringResource(R.string.eac3_atmos)
			"TRUEHD" -> stringResource(R.string.truehd_atmos)
			else -> stringResource(R.string.dolby_atmos)
		}

		profile?.contains("DTS:X", ignoreCase = true) == true -> stringResource(R.string.dts_x)
		profile.isDtsHd() -> stringResource(R.string.dts_hd)
		else -> when (codec) {
			"DCA" -> stringResource(R.string.dca)
			"AC3" -> stringResource(R.string.ac3)
			"EAC3" -> stringResource(R.string.eac3)
			"TRUEHD" -> stringResource(R.string.truehd)
			else -> codec
		}
	}
}

private fun String?.isDtsHd() = this != null && (contains("DTS:HD", ignoreCase = true) || contains("DTS-HD", ignoreCase = true))

@Composable
fun InfoRowMediaDetails(mediaSource: MediaSourceInfo) {
	val videoStream = mediaSource.mediaStreams?.getDefault(MediaStreamType.VIDEO)
	val audioStream = mediaSource.mediaStreams?.getDefault(MediaStreamType.AUDIO, mediaSource.defaultAudioStreamIndex)

	// Subtitles
	InfoRowSubtitleBadges(mediaSource.mediaStreams)

	// Video resolution
	if (videoStream?.width != null && videoStream.height != null) {
		InfoRowDefaultBadge(
			getResolutionName(
				width = videoStream.width ?: 0,
				height = videoStream.height ?: 0,
				interlaced = videoStream.isInterlaced,
			)
		)
	}

	if (mediaSource.video3dFormat != null) InfoRowDefaultBadge(stringResource(R.string.lbl_3d))

	// Video range
	InfoRowVideoRange(videoStream?.videoRangeType)

	// Audio stream
	val audioCodecName = audioCodecName(audioStream)
	if (!audioCodecName.isNullOrBlank()) InfoRowDefaultBadge(audioCodecName)

	// Audio channel layout
	val audioChannelLayout = audioStream?.channelLayout?.uppercase()
	if (!audioChannelLayout.isNullOrBlank()) InfoRowDefaultBadge(audioChannelLayout)
}
