package org.jellyfin.playback.core.queue

import org.jellyfin.playback.core.element.ElementKey
import org.jellyfin.playback.core.element.element
import kotlin.time.Duration

private val startPositionKey = ElementKey<Duration>("StartPosition")

/**
 * Velella: where playback of this [QueueEntry] starts (the resume point). The backend applies it once, when it starts the entry,
 * and clears it, so going back to the entry later starts it from the beginning.
 */
var QueueEntry.startPosition by element(startPositionKey)
