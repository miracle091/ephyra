package org.jellyfin.playback.core.mediastream

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.core.queue.startPosition
import org.jellyfin.playback.core.timedevent.TimedEvent
import org.jellyfin.playback.core.timedevent.addTimedEvent
import org.jellyfin.playback.core.timedevent.timedEvents
import timber.log.Timber
import kotlin.time.Duration

internal class MediaStreamService(
	private val mediaStreamResolvers: Collection<MediaStreamResolver>,
	private val preloadDuration: Duration,
) : PlayerService() {
	private companion object {
		private const val TIMED_EVENT_PRELOAD = "MediaStreamServicePreloadNext"
	}

	private var reloadJob: Job? = null

	override suspend fun onInitialize() {
		manager.queue.entry.onEach { entry ->
			Timber.d("Queue entry changed to $entry")
			reloadJob?.cancel()

			if (entry == null) {
				val backend = requireNotNull(manager.backend)
				backend.stop()
			} else {
				playEntry(entry)
				entry.ensurePreloadTimedEvent()
			}
		}.launchIn(coroutineScope + Dispatchers.Main)
	}

	private suspend fun QueueEntry.ensureMediaStream(): Boolean {
		mediaStream = mediaStream ?: resolveMediaStream()
		return mediaStream != null
	}

	// Ephyra: a cancelled request (newer reload, other entry) is rethrown, not taken for a failed one
	private suspend fun QueueEntry.resolveMediaStream(): PlayableMediaStream? = mediaStreamResolvers.firstNotNullOfOrNull { resolver ->
		runCatching {
			withContext(Dispatchers.IO) {
				resolver.getStream(this@resolveMediaStream)
			}
		}.onFailure {
			if (it is CancellationException) throw it
			Timber.e(it, "Media stream resolver failed for $this")
		}.getOrNull()
	}

	private fun QueueEntry.ensurePreloadTimedEvent() {
		if (preloadDuration <= Duration.ZERO) return

		val hasEvent = timedEvents?.any { it.key == TIMED_EVENT_PRELOAD } == true
		if (hasEvent) return

		addTimedEvent(
			TimedEvent.Block(
				key = TIMED_EVENT_PRELOAD,
				start = -preloadDuration,
				end = Duration.INFINITE,
				onActivate = { preloadNextEntry() }
			)
		)
	}

	private suspend fun playEntry(entry: QueueEntry) {
		val backend = requireNotNull(manager.backend)
		val hasMediaStream = entry.ensureMediaStream()

		if (hasMediaStream) {
			backend.playItem(entry)
		} else {
			Timber.e("Unable to resolve stream for entry $entry")

			// TODO: Somehow notify the user that we skipped an unplayable entry
			if (manager.queue.peekNext() != null) {
				manager.queue.next(usePlaybackOrder = true, useRepeatMode = false)
			} else {
				backend.stop()
			}
		}
	}

	/**
	 * Ephyra: asks the resolvers again for the current entry's stream (with the entry's new requests, like another audio track) and
	 * plays it on from the current position, paused if it was. The play session moves to the new stream, so the server gets a stop
	 * for the old one. A newer reload or another entry cancels it; on failure the current stream plays on.
	 */
	fun reload() {
		reloadJob?.cancel()
		reloadJob = coroutineScope.launch(Dispatchers.Main) {
			val entry = manager.queue.entry.value ?: return@launch
			val backend = requireNotNull(manager.backend)
			val wasPlaying = manager.state.playState.value == PlayState.PLAYING
			val position = backend.getPositionInfo().active
			Timber.i("Reloading the stream of $entry at $position")

			val reloaded = reloadEntryStream(
				entry = entry,
				position = position,
				isCurrent = { manager.queue.entry.value === entry },
				resolve = { entry.resolveMediaStream() },
			) {
				backend.playItem(entry)
				if (!wasPlaying) backend.pause()
			}
			if (!reloaded) Timber.w("Kept the current stream of $entry: no new stream, or playback moved on")
		}
	}

	private fun preloadNextEntry() = coroutineScope.launch(Dispatchers.Main) {
		// Peek into the next item to preload
		val nextItem = manager.queue.peekNext() ?: return@launch

		// Preload media stream information
		val hasMediaStream = nextItem.ensureMediaStream()

		if (hasMediaStream) {
			// Preload media in backend
			val backend = requireNotNull(manager.backend)
			backend.prepareItem(nextItem)
		}
	}
}

/**
 * Ephyra: the reload of [entry]'s stream. The new stream is asked for first ([resolve], with [position] as the entry's start, so
 * a transcode starts there) and replaces the old one, then [play]s, only if it came and [entry] still is the current entry
 * ([isCurrent]) with its old stream; otherwise the old stream plays on and the start is forgotten. Returns whether it replaced it.
 */
internal suspend fun reloadEntryStream(
	entry: QueueEntry,
	position: Duration,
	isCurrent: () -> Boolean,
	resolve: suspend () -> PlayableMediaStream?,
	play: () -> Unit,
): Boolean {
	// Still being asked for the first time: that request already reads the entry's new picks or not, nothing to replace
	val oldStream = entry.mediaStream ?: return false
	val start = position.takeIf { it > Duration.ZERO }
	entry.startPosition = start

	val newStream = try {
		resolve()
	} catch (error: CancellationException) {
		entry.startPosition = null
		throw error
	}

	if (newStream == null || !isCurrent() || entry.mediaStream !== oldStream) {
		entry.startPosition = null
		return false
	}

	// Set again: a cancelled older reload may have cleared it meanwhile
	entry.startPosition = start
	entry.mediaStream = newStream
	play()
	return true
}
