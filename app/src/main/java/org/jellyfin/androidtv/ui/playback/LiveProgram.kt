package org.jellyfin.androidtv.ui.playback

import java.time.Duration
import java.time.LocalDateTime

/**
 * Velella: the progress of the programme on air in the live TV player. The seek bar and the two times under it show the
 * programme's own start, end and elapsed share, never the position of the (time-shifted) stream.
 */
object LiveProgram {
	/** Length of the programme in milliseconds, or -1 when its start or end is unknown or the end is not after the start. */
	@JvmStatic
	fun durationMs(start: LocalDateTime?, end: LocalDateTime?): Long {
		if (start == null || end == null) return -1
		val duration = Duration.between(start, end).toMillis()
		return if (duration > 0) duration else -1
	}

	/** Elapsed time of the programme at [now], kept between 0 and its length. */
	@JvmStatic
	fun positionMs(start: LocalDateTime?, end: LocalDateTime?, now: LocalDateTime): Long {
		val duration = durationMs(start, end)
		if (start == null || duration < 0) return 0
		return Duration.between(start, now).toMillis().coerceIn(0, duration)
	}

	/** Whether the programme on air has finished at [now], so the next one has to be fetched. */
	@JvmStatic
	fun hasEnded(end: LocalDateTime?, now: LocalDateTime): Boolean = end != null && !now.isBefore(end)
}
