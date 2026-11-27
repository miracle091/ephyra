package org.jellyfin.playback.core.model

import org.jellyfin.playback.core.queue.StreamFallback

/** Velella: what the player did about a failed playback, for the UI to tell the user. */
sealed interface PlaybackFailure {
	/** The stream failed and the entry is played again with a heavier conversion ([fallback]) from the same position. */
	data class Retrying(val error: PlaybackError, val fallback: StreamFallback) : PlaybackFailure

	/** The stream failed too many times in a row (or no other stream came): playback stays stopped. */
	data class GaveUp(val error: PlaybackError) : PlaybackFailure

	/** The server gave no playable stream for an entry: the queue skipped it ([hasNext]) or stopped. */
	data class Unplayable(val hasNext: Boolean) : PlaybackFailure
}
