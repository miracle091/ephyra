package org.jellyfin.androidtv.ui.playback

import android.content.Context
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.compat.StreamInfo
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.TranscodeReason
import java.net.URLDecoder

/**
 * Ephyra: how the server delivers the current stream and why, for the line under the title in the player's top panel.
 * The server lists its reasons in the TranscodeReasons parameter of the transcoding URL.
 */
object PlayInfo {
	private val transcodeReasonsParameter = Regex("[?&]TranscodeReasons=([^&]*)", RegexOption.IGNORE_CASE)

	fun transcodeReasons(transcodingUrl: String?): List<TranscodeReason> {
		val value = transcodingUrl?.let { transcodeReasonsParameter.find(it) }?.groupValues?.get(1) ?: return emptyList()
		// The Charset overload needs API 33; the String one works on every Shield (Android 11).
		return URLDecoder.decode(value, Charsets.UTF_8.name())
			.split(',')
			.mapNotNull { TranscodeReason.fromNameOrNull(it.trim()) }
	}

	fun describe(context: Context, playMethod: PlayMethod, transcodingUrl: String?): String {
		val method = context.getString(
			when (playMethod) {
				PlayMethod.DIRECT_PLAY -> R.string.ephyra_play_info_direct_play
				PlayMethod.DIRECT_STREAM -> R.string.ephyra_play_info_direct_stream
				PlayMethod.TRANSCODE -> R.string.ephyra_play_info_transcode
			}
		)
		if (playMethod == PlayMethod.DIRECT_PLAY) return method

		val reasons = transcodeReasons(transcodingUrl).map { context.getString(it.label) }
		return if (reasons.isEmpty()) method else context.getString(R.string.ephyra_play_info_with_reasons, method, reasons.joinToString())
	}

	/** Shows the line for [streamInfo] in [view], or hides the view while no stream is known. */
	@JvmStatic
	fun bind(view: TextView, streamInfo: StreamInfo?) {
		val text = streamInfo?.let { describe(view.context, it.playMethod, it.mediaSource?.transcodingUrl) }
		view.text = text
		view.isVisible = text != null
	}

	@get:StringRes
	private val TranscodeReason.label: Int
		get() = when (this) {
			TranscodeReason.CONTAINER_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_container
			TranscodeReason.VIDEO_CODEC_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_video_codec
			TranscodeReason.AUDIO_CODEC_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_audio_codec
			TranscodeReason.SUBTITLE_CODEC_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_subtitle_codec
			TranscodeReason.AUDIO_IS_EXTERNAL -> R.string.ephyra_transcode_reason_audio_external
			TranscodeReason.SECONDARY_AUDIO_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_secondary_audio
			TranscodeReason.VIDEO_PROFILE_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_video_profile
			TranscodeReason.VIDEO_LEVEL_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_video_level
			TranscodeReason.VIDEO_RESOLUTION_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_video_resolution
			TranscodeReason.VIDEO_BIT_DEPTH_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_video_bit_depth
			TranscodeReason.VIDEO_FRAMERATE_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_video_framerate
			TranscodeReason.REF_FRAMES_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_ref_frames
			TranscodeReason.ANAMORPHIC_VIDEO_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_anamorphic
			TranscodeReason.INTERLACED_VIDEO_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_interlaced
			TranscodeReason.AUDIO_CHANNELS_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_audio_channels
			TranscodeReason.AUDIO_PROFILE_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_audio_profile
			TranscodeReason.AUDIO_SAMPLE_RATE_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_audio_sample_rate
			TranscodeReason.AUDIO_BIT_DEPTH_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_audio_bit_depth
			TranscodeReason.CONTAINER_BITRATE_EXCEEDS_LIMIT -> R.string.ephyra_transcode_reason_container_bitrate
			TranscodeReason.VIDEO_BITRATE_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_video_bitrate
			TranscodeReason.AUDIO_BITRATE_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_audio_bitrate
			TranscodeReason.UNKNOWN_VIDEO_STREAM_INFO -> R.string.ephyra_transcode_reason_unknown_video
			TranscodeReason.UNKNOWN_AUDIO_STREAM_INFO -> R.string.ephyra_transcode_reason_unknown_audio
			TranscodeReason.DIRECT_PLAY_ERROR -> R.string.ephyra_transcode_reason_direct_play_error
			TranscodeReason.VIDEO_RANGE_TYPE_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_video_range
			TranscodeReason.VIDEO_CODEC_TAG_NOT_SUPPORTED -> R.string.ephyra_transcode_reason_video_codec_tag
			TranscodeReason.STREAM_COUNT_EXCEEDS_LIMIT -> R.string.ephyra_transcode_reason_stream_count
		}
}
