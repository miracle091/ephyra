package org.jellyfin.playback.jellyfin.queue

import org.jellyfin.playback.core.element.ElementKey
import org.jellyfin.playback.core.element.element
import org.jellyfin.playback.core.queue.QueueEntry

private val subtitleStreamIndexKey = ElementKey<Int>("SubtitleStreamIndex")

/**
 * Velella: get or set the index (Jellyfin media stream index) of the subtitle track to request from the server, -1 for no
 * subtitles, or null to let the server choose from the user's subtitle settings.
 */
var QueueEntry.subtitleStreamIndex by element(subtitleStreamIndexKey)
