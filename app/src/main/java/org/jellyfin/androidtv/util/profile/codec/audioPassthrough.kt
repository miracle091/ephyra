package org.jellyfin.androidtv.util.profile.codec

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioCapabilities

@OptIn(UnstableApi::class)
fun isPassthroughAudioAvailable(context: Context, mimetype: String): Boolean {
	val attributes = AudioAttributes.Builder()
		.setUsage(C.USAGE_MEDIA)
		.setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
		.build()

	val capabilities = AudioCapabilities.getCapabilities(
		context,
		attributes,
		null,
		listOf(AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.CHANNEL_OUT_5POINT1)
	)

	val format = Format.Builder()
		.setSampleMimeType(mimetype)
		.setChannelCount(Integer.bitCount(AudioFormat.CHANNEL_OUT_STEREO))
		.setSampleRate(Format.NO_VALUE)
		.build()

	return capabilities.isPassthroughPlaybackSupported(format, attributes)
}

/**
 * Ephyra: "can pass through" and "can decode" are different questions. A device whose HDMI sink refuses an AC3/E-AC3/DTS/TrueHD
 * bitstream can still play it when the platform has a hardware or vendor MediaCodec decoder for the format (the player then outputs
 * PCM). Software-only platform decoders and the bundled FFmpeg decoder do not count: those formats are left to the server to transcode.
 */
fun hasPlatformAudioDecoder(
	mimeType: String,
	codecInfos: Array<MediaCodecInfo> = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos,
): Boolean = codecInfos.any { info ->
	!info.isEncoder && !info.isSoftwareOnly && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
}
