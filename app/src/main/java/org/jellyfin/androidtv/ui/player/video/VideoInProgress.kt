package org.jellyfin.androidtv.ui.player.video

import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.mediatype.MediaType
import org.jellyfin.playback.core.mediastream.mediatype.mediaType
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue

/**
 * Ephyra: whether the new player plays or pauses a video: another client's "display content" must not interrupt it, and a new
 * playback replaces its screen instead of opening a second player on top.
 */
fun isVideoInProgress(playState: PlayState, entry: QueueEntry?): Boolean =
	entry?.mediaType == MediaType.Video && (playState == PlayState.PLAYING || playState == PlayState.PAUSED)

/** Ephyra: [isVideoInProgress] for this player now. */
val PlaybackManager.isVideoInProgress: Boolean
	get() = isVideoInProgress(state.playState.value, queue.entry.value)
