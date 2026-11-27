package org.jellyfin.playback.jellyfin.queue

import org.jellyfin.playback.core.element.ElementKey
import org.jellyfin.playback.core.element.element
import org.jellyfin.playback.core.queue.QueueEntry

private val audioStreamIndexKey = ElementKey<Int>("AudioStreamIndex")

/**
 * Velella: get or set the index (Jellyfin media stream index) of the audio track to request from the server, or null to let the
 * server choose from the user's language preferences.
 */
var QueueEntry.audioStreamIndex by element(audioStreamIndexKey)
