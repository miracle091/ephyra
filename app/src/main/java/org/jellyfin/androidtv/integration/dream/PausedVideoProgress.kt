package org.jellyfin.androidtv.integration.dream

import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Velella: where a paused video stands, for the long pause screen. */
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

private const val MS_PER_SECOND = 1000
private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3600

/** A playback time as the player shows it: 4:05, 24:20 or 1:02:03. */
fun formatPlaybackTime(ms: Long): String {
	val totalSeconds = ms.coerceAtLeast(0) / MS_PER_SECOND
	val hours = totalSeconds / SECONDS_PER_HOUR
	val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
	val seconds = totalSeconds % SECONDS_PER_MINUTE
	return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
