package org.jellyfin.playback.core.model

/**
 * Why playback failed, in terms the UI and the retry logic can act on: a [Reason.SOURCE] error may go away with another
 * stream (direct stream, transcode), a [Reason.DECODER] one asks for a transcode.
 */
data class PlaybackError(
	val reason: Reason,
	/** The backend's own description, for logs and diagnostics. */
	val message: String?,
) {
	enum class Reason {
		/** The stream could not be read or parsed (network, server, malformed container). */
		SOURCE,

		/** The device could not decode the stream. */
		DECODER,

		/** The audio output failed (audio track init or write, passthrough). */
		AUDIO_OUTPUT,

		OTHER,
	}
}
