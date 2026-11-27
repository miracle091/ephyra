package org.jellyfin.androidtv.ui.player.video

import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.mediatype.MediaType
import org.jellyfin.playback.core.mediastream.mediatype.mediaType
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue

/**
 * Velella: whether the new player plays or pauses a video: another client's "display content" must not interrupt it, and a new
 * playback replaces its screen instead of opening a second player on top.
 */
fun isVideoInProgress(playState: PlayState, entry: QueueEntry?): Boolean =
	entry?.mediaType == MediaType.Video && (playState == PlayState.PLAYING || playState == PlayState.PAUSED)

/** Velella: whether the new player holds a paused video (the screensaver comes sooner then, as with the legacy player). */
fun isVideoPaused(playState: PlayState, entry: QueueEntry?): Boolean =
	entry?.mediaType == MediaType.Video && playState == PlayState.PAUSED

/** Velella: [isVideoPaused] for this player now. */
val PlaybackManager.isVideoPaused: Boolean
	get() = isVideoPaused(state.playState.value, queue.entry.value)

/** Velella: [isVideoInProgress] for this player now. */
val PlaybackManager.isVideoInProgress: Boolean
	get() = isVideoInProgress(state.playState.value, queue.entry.value)
