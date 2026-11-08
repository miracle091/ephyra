package org.jellyfin.androidtv.ui.playback

/**
 * Ephyra: opening a live channel's stream can fail for a moment (the server answers 500 while its transcode is not ready,
 * mostly right after a channel switch). A live stream is retried a few times with a growing pause before the failure counts
 * as a playback error; anything else keeps the player's normal error handling.
 */
object LiveStreamRetry {
	const val MAX_ATTEMPTS = 3
	private const val STEP_MS = 1_000L
	// A transcoded channel took about 15 s to start on the Shield; twice that leaves room for a slow server
	private const val BUFFER_TIMEOUT_MS = 30_000L

	/** Whether a live stream that failed after [attemptsDone] retries is tried again. */
	@JvmStatic
	fun shouldRetry(isLiveTv: Boolean, attemptsDone: Int): Boolean = isLiveTv && attemptsDone < MAX_ATTEMPTS

	/** The pause before the retry number [attempt] (1 for the first): 1 s, 2 s, 3 s. */
	@JvmStatic
	fun delayMs(attempt: Int): Long = STEP_MS * attempt.coerceAtLeast(1)

	/**
	 * How long a live stream may stay in BUFFERING after its video path is set before it counts as failed: some channels leave
	 * the player buffering forever, without an error. 0 when no timeout applies (anything but live TV).
	 */
	@JvmStatic
	fun bufferTimeoutMs(isLiveTv: Boolean): Long = if (isLiveTv) BUFFER_TIMEOUT_MS else 0L

	/** Whether the elapsed buffering timeout is a failure: a live stream that is still buffering, not one that started or stopped. */
	@JvmStatic
	fun shouldFailOnBufferTimeout(isLiveTv: Boolean, isBuffering: Boolean): Boolean = isLiveTv && isBuffering
}
