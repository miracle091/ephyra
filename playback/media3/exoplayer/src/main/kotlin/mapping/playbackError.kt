package org.jellyfin.playback.media3.exoplayer.mapping

import androidx.media3.common.PlaybackException
import org.jellyfin.playback.core.model.PlaybackError

// Media3 groups its error codes by thousands (PlaybackException): 2xxx input/output, 3xxx parsing, 4xxx decoding, 5xxx audio track
private val SOURCE_ERROR_CODES = PlaybackException.ERROR_CODE_IO_UNSPECIFIED until PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
private val DECODER_ERROR_CODES = PlaybackException.ERROR_CODE_DECODER_INIT_FAILED until PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
private val AUDIO_OUTPUT_ERROR_CODES = PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED until PlaybackException.ERROR_CODE_DRM_UNSPECIFIED

/** Ephyra: the [PlaybackError.Reason] of a Media3 [PlaybackException.errorCode]. */
fun playbackErrorReason(errorCode: Int): PlaybackError.Reason = when (errorCode) {
	in SOURCE_ERROR_CODES -> PlaybackError.Reason.SOURCE
	in DECODER_ERROR_CODES -> PlaybackError.Reason.DECODER
	in AUDIO_OUTPUT_ERROR_CODES -> PlaybackError.Reason.AUDIO_OUTPUT
	else -> PlaybackError.Reason.OTHER
}

fun PlaybackException.toPlaybackError() = PlaybackError(
	reason = playbackErrorReason(errorCode),
	message = "$errorCodeName: $message",
)
