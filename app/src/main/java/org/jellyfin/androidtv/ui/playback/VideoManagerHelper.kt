package org.jellyfin.androidtv.ui.playback

import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import androidx.core.net.toUri
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.playback.media3.exoplayer.mapping.getFfmpegSubtitleMimeType
import org.jellyfin.sdk.model.api.MediaStream
import timber.log.Timber

/**
 * Return the media type for the codec found in this media stream. First tries to infer the media type from the streams delivery URL and
 * falls back to the original stream codec.
 */
fun getSubtitleMediaStreamCodec(stream: MediaStream): String {
	val codec = requireNotNull(stream.codec)
	val codecMediaType = getFfmpegSubtitleMimeType(codec, "").ifBlank { null }

	val urlSubtitleExtension = stream.deliveryUrl?.toUri()?.lastPathSegment?.split('.')?.last()
	val urlExtensionMediaType = urlSubtitleExtension?.let { getFfmpegSubtitleMimeType(it, "") }?.ifBlank { null }

	return urlExtensionMediaType ?: codecMediaType ?: urlSubtitleExtension ?: codec
}

private const val LIMITER_ATTACK_MS = 30f
private const val LIMITER_RELEASE_MS = 300f
private const val LIMITER_RATIO = 10f
private const val LIMITER_THRESHOLD_DB = -24f
private const val LIMITER_POST_GAIN_DB = 3f
private const val EQ_BAND_COUNT = 5
private const val EQ_GAIN_LOW = 0.02f
private const val EQ_GAIN_HIGH = 0.03f
private const val EQ_LEVEL_LOW: Short = 2
private const val EQ_LEVEL_HIGH: Short = 3

// Night mode curve: a gentle boost of the lower-mid bands, per band
private val NIGHTMODE_EQ_GAINS = floatArrayOf(0f, EQ_GAIN_LOW, EQ_GAIN_HIGH, EQ_GAIN_LOW, 0f)
private val NIGHTMODE_EQ_LEVELS = shortArrayOf(0, EQ_LEVEL_LOW, EQ_LEVEL_HIGH, EQ_LEVEL_LOW, 0)

private var audioEffect: AudioEffect? = null

fun applyAudioNightmode(audioSessionId: Int) {
	Timber.i("Enabling audio night mode for session $audioSessionId")

	audioEffect?.release()

	audioEffect = when {
		// Use dynamics processinc on Android 9 (API 28) and newer
		AndroidVersion.isAtLeastP -> DynamicsProcessing(0, audioSessionId, null).apply {
			setLimiterAllChannelsTo(
				DynamicsProcessing.Limiter(
					true,
					true,
					1,
					LIMITER_ATTACK_MS,
					LIMITER_RELEASE_MS,
					LIMITER_RATIO,
					LIMITER_THRESHOLD_DB,
					LIMITER_POST_GAIN_DB
				)
			)

			setPreEqAllChannelsTo(DynamicsProcessing.Eq(true, true, EQ_BAND_COUNT).apply {
				NIGHTMODE_EQ_GAINS.forEachIndexed { band, gain -> getBand(band).gain = gain }
			})

			enabled = true
		}

		// Use more simple equalizer implementation on older versions
		else -> Equalizer(0, audioSessionId).apply {
			NIGHTMODE_EQ_LEVELS.forEachIndexed { band, level -> setBandLevel(band.toShort(), level) }
			enabled = true
		}
	}
}
