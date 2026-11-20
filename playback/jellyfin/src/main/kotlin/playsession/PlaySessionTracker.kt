package org.jellyfin.playback.jellyfin.playsession

import org.jellyfin.playback.core.model.PlayState
import kotlin.time.Duration

/** Ephyra: a report [PlaySessionTracker] asks the server for. */
sealed interface PlaySessionReport<out T> {
	val session: T

	data class Start<T>(override val session: T) : PlaySessionReport<T>
	data class Progress<T>(override val session: T) : PlaySessionReport<T>
	data class Stop<T>(override val session: T, val position: Duration, val failed: Boolean) : PlaySessionReport<T>
}

/**
 * Ephyra: decides which playback reports to send, so every start gets its stop. The session that was started is kept here
 * rather than read back from the queue, because stopping the player clears the queue before the stop report could read it,
 * and moving to the next entry while playing changes no play state. [T] is the reported session (entry, stream and item).
 */
class PlaySessionTracker<T : Any> {
	var active: T? = null
		private set

	private var lastPosition: Duration = Duration.ZERO

	/** Remembers the started session's [position] between reports, so a stop reported after the queue was cleared is exact. */
	fun observe(current: T?, position: Duration) {
		if (current != null && current == active) lastPosition = position
	}

	/**
	 * [current] is the queue entry being played, or null when it has no stream yet or the queue is empty; [position] its
	 * position, or null when unknown. Called on every play state and queue entry change and on each periodic tick.
	 */
	fun update(current: T?, playState: PlayState, position: Duration?): List<PlaySessionReport<T>> {
		val reports = mutableListOf<PlaySessionReport<T>>()

		val started = active
		if (started != null && current == started && position != null) lastPosition = position

		stop(started, current, playState)?.let { reports += it }

		val running = active
		when {
			// Pause, unpause and ticks: the server learns the position and the paused flag
			running != null -> reports += PlaySessionReport.Progress(running)

			current != null && shouldStart(started, current, playState) -> {
				reports += PlaySessionReport.Start(current)
				active = current
				lastPosition = position ?: Duration.ZERO
			}
		}

		return reports
	}

	/** The stop of the [started] session when another entry replaced it or playback ended or failed. */
	private fun stop(started: T?, current: T?, playState: PlayState): PlaySessionReport.Stop<T>? {
		val ended = playState == PlayState.STOPPED || playState == PlayState.ERROR
		if (started == null || (current == started && !ended)) return null
		active = null
		return PlaySessionReport.Stop(started, lastPosition, failed = playState == PlayState.ERROR)
	}

	/**
	 * Whether [current] starts a session: when it plays, or when it replaced the [started] one while paused (a track switch reload),
	 * so the server keeps the session.
	 */
	private fun shouldStart(started: T?, current: T, playState: PlayState): Boolean = when (playState) {
		PlayState.PLAYING -> true
		PlayState.PAUSED -> started != null && current != started
		PlayState.STOPPED, PlayState.ERROR -> false
	}
}
