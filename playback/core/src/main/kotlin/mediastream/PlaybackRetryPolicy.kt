package org.jellyfin.playback.core.mediastream

import org.jellyfin.playback.core.model.PlaybackError
import org.jellyfin.playback.core.queue.StreamFallback

/**
 * Velella: the legacy player's retry chain for the current entry. The first error drops direct play, the second asks for a transcode,
 * the third gives up; a decoder error goes to a transcode at once, since a remux carries the same codec. Errors more than
 * [STABLE_MS] apart count again from the first attempt, and an entry never steps back up to a lighter stream.
 */
internal class PlaybackRetryPolicy(
	private val clock: () -> Long = System::currentTimeMillis,
) {
	companion object {
		const val MAX_ATTEMPTS = 3
		const val STABLE_MS = 30_000L
	}

	private var attempts = 0
	private var lastErrorAt = 0L

	/** The stream to retry with after an error of [reason], given the entry's [current] fallback, or null to give up. */
	fun onError(reason: PlaybackError.Reason, current: StreamFallback?): StreamFallback? {
		val now = clock()
		if (attempts > 0 && now - lastErrorAt > STABLE_MS) attempts = 0
		attempts++
		lastErrorAt = now
		if (attempts >= MAX_ATTEMPTS) return null

		val byAttempt = if (attempts == 1) StreamFallback.NO_DIRECT_PLAY else StreamFallback.TRANSCODE
		val byReason = when (reason) {
			PlaybackError.Reason.DECODER -> StreamFallback.TRANSCODE
			PlaybackError.Reason.SOURCE,
			PlaybackError.Reason.AUDIO_OUTPUT,
			PlaybackError.Reason.OTHER -> StreamFallback.NONE
		}
		return maxOf(byAttempt, byReason, current ?: StreamFallback.NONE)
	}

	/** Forgets the attempts, for a new entry. */
	fun reset() {
		attempts = 0
		lastErrorAt = 0L
	}
}
