package org.jellyfin.androidtv.ui.playback

/**
 * Ephyra: opening a live channel's stream can fail for a moment (the server answers 500 while its transcode is not ready,
 * mostly right after a channel switch). A live stream is retried a few times with a growing pause before the failure counts
 * as a playback error; anything else keeps the player's normal error handling.
 */
object LiveStreamRetry {
	const val MAX_ATTEMPTS = 3
	private const val STEP_MS = 1_000L

	/** Whether a live stream that failed after [attemptsDone] retries is tried again. */
	@JvmStatic
	fun shouldRetry(isLiveTv: Boolean, attemptsDone: Int): Boolean = isLiveTv && attemptsDone < MAX_ATTEMPTS

	/** The pause before the retry number [attempt] (1 for the first): 1 s, 2 s, 3 s. */
	@JvmStatic
	fun delayMs(attempt: Int): Long = STEP_MS * attempt.coerceAtLeast(1)
}
