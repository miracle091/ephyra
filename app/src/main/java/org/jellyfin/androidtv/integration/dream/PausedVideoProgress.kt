package org.jellyfin.androidtv.integration.dream

import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Ephyra: where a paused video stands, for the long pause screen. */
data class PausedVideoProgress(
	val fraction: Float,
	val remaining: Duration,
	val endsAt: LocalTime,
)

fun pausedVideoProgress(positionMs: Long, durationMs: Long, now: LocalTime): PausedVideoProgress {
	val duration = durationMs.coerceAtLeast(0)
	val position = positionMs.coerceIn(0, duration)
	val remaining = (duration - position).milliseconds
	return PausedVideoProgress(
		fraction = if (duration == 0L) 0f else position.toFloat() / duration,
		remaining = remaining,
		endsAt = now.plusSeconds(remaining.inWholeSeconds),
	)
}

/** A playback time as the player shows it: 4:05, 24:20 or 1:02:03. */
fun formatPlaybackTime(ms: Long): String {
	val totalSeconds = ms.coerceAtLeast(0) / 1000
	val hours = totalSeconds / 3600
	val minutes = totalSeconds % 3600 / 60
	val seconds = totalSeconds % 60
	return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
