package org.jellyfin.androidtv.integration.dream

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.mediatype.MediaType
import org.jellyfin.playback.core.mediastream.mediatype.mediaType
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem

class DreamViewModel(
	private val playbackManager: PlaybackManager,
	private val playbackControllerContainer: PlaybackControllerContainer,
) : ViewModel() {
	// Ephyra: a video paused in the player, else what is playing, else the clock (no library showcase)
	val content: Flow<DreamContent> = playbackManager.queue.entry.map(::contentFor)

	/**
	 * Ephyra: the content right now, for the screensaver's first frame. The view model outlives each showing, so a held value would
	 * be the last showing's (or the clock) and fade into the right one a second later.
	 */
	fun currentContent(): DreamContent = contentFor(playbackManager.queue.entry.value)

	private fun contentFor(entry: QueueEntry?): DreamContent = pausedVideo()
		?: entry
			?.takeIf { it.visibleInScreensaver }
			?.baseItem
			?.let { baseItem -> DreamContent.NowPlaying(entry, baseItem) }
		?: DreamContent.Clock

	private fun pausedVideo(): DreamContent.VideoPaused? = legacyPausedVideo() ?: pausedVideoContent(
		playState = playbackManager.state.playState.value,
		entry = playbackManager.queue.entry.value,
		position = { playbackManager.state.positionInfo },
	)

	private fun legacyPausedVideo(): DreamContent.VideoPaused? {
		val controller = playbackControllerContainer.playbackController ?: return null
		if (!controller.isPaused || controller.isLiveTv) return null
		val item = controller.currentlyPlayingItem ?: return null
		return DreamContent.VideoPaused(item, controller.currentPosition, controller.duration)
	}
}

/** Ephyra: the new player's paused video for the screensaver, or null when it plays no video or isn't paused. */
internal fun pausedVideoContent(playState: PlayState, entry: QueueEntry?, position: () -> PositionInfo): DreamContent.VideoPaused? {
	if (playState != PlayState.PAUSED || entry?.mediaType != MediaType.Video) return null
	val item = entry.baseItem ?: return null
	val info = position()
	return DreamContent.VideoPaused(item, info.active.inWholeMilliseconds, info.duration.inWholeMilliseconds)
}
