package org.jellyfin.playback.media3.exoplayer.mapping

import androidx.media3.common.PlaybackException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.core.model.PlaybackError

class PlaybackErrorMappingTests : FunSpec({
	listOf(
		PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED to PlaybackError.Reason.SOURCE,
		PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS to PlaybackError.Reason.SOURCE,
		PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED to PlaybackError.Reason.SOURCE,
		PlaybackException.ERROR_CODE_DECODER_INIT_FAILED to PlaybackError.Reason.DECODER,
		PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED to PlaybackError.Reason.DECODER,
		PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED to PlaybackError.Reason.AUDIO_OUTPUT,
		PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED to PlaybackError.Reason.AUDIO_OUTPUT,
		PlaybackException.ERROR_CODE_DRM_UNSPECIFIED to PlaybackError.Reason.OTHER,
		PlaybackException.ERROR_CODE_UNSPECIFIED to PlaybackError.Reason.OTHER,
	).forEach { (code, reason) ->
		test("Media3 error code $code maps to $reason") {
			playbackErrorReason(code) shouldBe reason
		}
	}
})
