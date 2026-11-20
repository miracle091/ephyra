package org.jellyfin.playback.jellyfin.mediasegment

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.core.timedevent.BlockActivation
import org.jellyfin.playback.core.timedevent.TimedEvent
import org.jellyfin.playback.core.timedevent.timedEvents
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.mediaSegmentApi
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.extensions.ticks
import timber.log.Timber
import kotlin.time.Duration

class MediaSegmentService(
	private val api: ApiClient,
	// Ephyra: the user's action for each segment (Settings, media segments) decides, not a fixed set of types
	private val shouldSkip: (MediaSegmentDto) -> Boolean,
) : PlayerService() {
	companion object {
		const val TIMED_EVENT_PREFIX = "MediaSegment:"
	}

	override suspend fun onInitialize() {
		// Load media segments for an item as soon as it becomes the currently playing item
		manager.queue.entry
			.filterNotNull()
			.onEach { entry -> fetchMediaSegments(entry) }
			.onEach { entry -> createTimedEvents(entry) }
			.launchIn(coroutineScope)
	}

	private suspend fun fetchMediaSegments(entry: QueueEntry) {
		// Already has media segments!
		if (entry.mediaSegments != null) return

		// BaseItem doesn't exist
		val baseItem = entry.baseItem ?: return

		// Get via API; Ephyra: a failed request no longer ends the collection, so later entries still get their segments
		runCatching { api.mediaSegmentApi.getItemSegments(baseItem.id).content.items }
			.onSuccess { entry.mediaSegments = it }
			.onFailure { error ->
				if (error is CancellationException) throw error
				Timber.w(error, "Failed to load the media segments of ${baseItem.id}")
			}
	}

	private fun createTimedEvents(entry: QueueEntry) {
		val events = entry.timedEvents
			?.toMutableList()
			?: mutableListOf()

		// Remove existing media segments
		events.removeAll { event -> event.key?.startsWith(TIMED_EVENT_PREFIX) == true }

		// Add current media segments
		val mediaSegments = entry.mediaSegments.orEmpty()
		for (mediaSegment in mediaSegments) {
			for (timedEvents in mediaSegment.asTimedEvents()) {
				events.add(timedEvents)
			}
		}

		// Set new timed events
		entry.timedEvents = events.ifEmpty { null }
	}

	private fun MediaSegmentDto.asTimedEvents(): List<TimedEvent> = skipEvents(shouldSkip) { end ->
		coroutineScope.launch(Dispatchers.Main) {
			manager.state.seek(end)
		}
	}
}

/**
 * The timed events that skip this segment when [shouldSkip] says so: reaching its start while playing calls [skipTo] with its
 * end; seeking into it does not (the user went there on purpose).
 */
internal fun MediaSegmentDto.skipEvents(shouldSkip: (MediaSegmentDto) -> Boolean, skipTo: (Duration) -> Unit): List<TimedEvent> {
	if (!shouldSkip(this)) return emptyList()

	return listOf(
		TimedEvent.Block(
			key = "${MediaSegmentService.TIMED_EVENT_PREFIX}$id",
			start = startTicks.ticks,
			end = endTicks.ticks,
			onActivate = { metadata ->
				if (metadata !is BlockActivation.Natural) return@Block
				skipTo(endTicks.ticks)
			}
		)
	)
}
