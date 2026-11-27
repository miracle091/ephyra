package org.jellyfin.playback.core.mediastream

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackError
import org.jellyfin.playback.core.model.PlaybackFailure
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.core.queue.startPosition
import org.jellyfin.playback.core.queue.streamFallback
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
		private const val FAILURE_BUFFER = 8
	}

	private var reloadJob: Job? = null
	private var liveStartJob: Job? = null
	private var retryReloadJob: Job? = null
	private val retryPolicy = PlaybackRetryPolicy()

	private val _failures = MutableSharedFlow<PlaybackFailure>(extraBufferCapacity = FAILURE_BUFFER)

	/** Velella: what the player did about each failed playback, for the UI. */
	val failures: SharedFlow<PlaybackFailure> = _failures.asSharedFlow()

	override suspend fun onInitialize() {
		// Velella: the backend's errors, each one (the play state stays ERROR when a retry fails again, so it can't tell them apart)
		manager.backendService.addListener(object : PlayerBackendEventListener() {
			override fun onPlayError(error: PlaybackError) {
				coroutineScope.launch(Dispatchers.Main) { onPlaybackError(error) }
			}
		})

		// Velella: the retry chain counts again only after the stream really played for a while
		manager.state.playState.onEach { state ->
			if (state == PlayState.PLAYING) retryPolicy.onPlaying()
		}.launchIn(coroutineScope + Dispatchers.Main)

		manager.queue.entry.onEach { entry ->
			Timber.d("Queue entry changed to $entry")
			reloadJob?.cancel()
			liveStartJob?.cancel()
			retryPolicy.reset()
			// Playing on from the previous entry (gapless): no new play state comes
			if (manager.state.playState.value == PlayState.PLAYING) retryPolicy.onPlaying()

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

	// Velella: a cancelled request (newer reload, other entry) is rethrown, not taken for a failed one
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
			watchLiveStart(entry)
		} else {
			Timber.e("Unable to resolve stream for entry $entry")

			// Velella: the UI tells the user that the entry was skipped, or closes the player
			val hasNext = manager.queue.peekNext() != null
			_failures.tryEmit(PlaybackFailure.Unplayable(hasNext))
			if (hasNext) {
				manager.queue.next(usePlaybackOrder = true, useRepeatMode = false)
			} else {
				backend.stop()
			}
		}
	}

	/**
	 * Velella: a live stream that neither plays nor fails within [LIVE_START_TIMEOUT] counts as a stream error, so the retry chain
	 * tries it again with a heavier stream or gives up, as the legacy player does for channels that buffer forever.
	 */
	private fun watchLiveStart(entry: QueueEntry) {
		liveStartJob?.cancel()
		if (entry.mediaStream?.isLive != true) return
		liveStartJob = coroutineScope.launch(Dispatchers.Main) {
			if (!liveStartStalled(manager.state.playState)) return@launch
			if (manager.queue.entry.value !== entry) return@launch
			Timber.w("Live stream of $entry did not start within $LIVE_START_TIMEOUT")
			onPlaybackError(PlaybackError(PlaybackError.Reason.SOURCE, "Live stream did not start within $LIVE_START_TIMEOUT"))
		}
	}

	/** Velella: the legacy retry chain ([PlaybackRetryPolicy]): the same entry again with a heavier stream from the same position. */
	private fun onPlaybackError(error: PlaybackError) {
		val entry = manager.queue.entry.value ?: return
		// A late error of the stream a retry is already replacing must not count as another attempt
		if (retryReloadJob?.isActive == true) {
			Timber.i("Ignoring a ${error.reason} error while the retry's new stream loads")
			return
		}
		val fallback = retryPolicy.onError(error.reason, entry.streamFallback)
		if (fallback == null) {
			Timber.w("Giving up on $entry after repeated errors (${error.reason})")
			_failures.tryEmit(PlaybackFailure.GaveUp(error))
			return
		}

		Timber.i("Retrying $entry with $fallback after a ${error.reason} error")
		entry.streamFallback = fallback
		_failures.tryEmit(PlaybackFailure.Retrying(error, fallback))
		reload(resume = true) {
			Timber.w("No other stream for $entry")
			_failures.tryEmit(PlaybackFailure.GaveUp(error))
		}
		retryReloadJob = reloadJob
	}

	/**
	 * Velella: asks the resolvers again for the current entry's stream (with the entry's new requests, like another audio track) and
	 * plays it on from [position] (the current one by default), paused if it was unless [resume] says otherwise. The play session
	 * moves to the new stream, so the server gets a stop for the old one. A newer reload or another entry cancels it; on failure the
	 * current stream plays on and [onKept] runs.
	 */
	fun reload(position: Duration? = null, resume: Boolean? = null, onKept: () -> Unit = {}) {
		reloadJob?.cancel()
		reloadJob = coroutineScope.launch(Dispatchers.Main) {
			val entry = manager.queue.entry.value ?: return@launch
			val backend = requireNotNull(manager.backend)
			val play = resume ?: (manager.state.playState.value == PlayState.PLAYING)
			val start = position ?: backend.getPositionInfo().active
			Timber.i("Reloading the stream of $entry at $start")

			val reloaded = reloadEntryStream(
				entry = entry,
				position = start,
				isCurrent = { manager.queue.entry.value === entry },
				resolve = { entry.resolveMediaStream() },
			) {
				backend.playItem(entry)
				if (!play) backend.pause() else watchLiveStart(entry)
			}
			if (!reloaded) {
				Timber.w("Kept the current stream of $entry: no new stream, or playback moved on")
				if (manager.queue.entry.value === entry) onKept()
			}
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
 * Velella: the reload of [entry]'s stream. The new stream is asked for first ([resolve], with [position] as the entry's start, so
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
