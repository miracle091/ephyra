package org.jellyfin.playback.core.queue

import org.jellyfin.playback.core.element.ElementKey
import org.jellyfin.playback.core.element.element

/** Velella: how far a [QueueEntry]'s stream steps down after playback errors; each step asks the server for a heavier conversion. */
enum class StreamFallback {
	/** Anything the server offers: direct play first. */
	NONE,

	/** No direct play: the server remuxes (direct stream) or transcodes. */
	NO_DIRECT_PLAY,

	/** Only a transcode. */
	TRANSCODE,
}

private val streamFallbackKey = ElementKey<StreamFallback>("StreamFallback")

/** Velella: the stream this entry falls back to after playback errors; null until the first error. */
var QueueEntry.streamFallback by element(streamFallbackKey)

/** Whether the server may still offer direct play under this fallback (null: no error yet). */
val StreamFallback?.allowsDirectPlay get() = this == null || this == StreamFallback.NONE

/** Whether the server may still offer a remux (direct stream) under this fallback (null: no error yet). */
val StreamFallback?.allowsDirectStream get() = this != StreamFallback.TRANSCODE
