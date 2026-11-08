package org.jellyfin.androidtv.integration.dream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem

class DreamViewModel(
	playbackManager: PlaybackManager,
) : ViewModel() {
	// Ephyra: what is playing, else the clock (no library showcase)
	@OptIn(ExperimentalCoroutinesApi::class)
	val content = playbackManager.queue.entry
		.map { entry ->
			entry
				?.takeIf { it.visibleInScreensaver }
				?.baseItem
				?.let { baseItem -> DreamContent.NowPlaying(entry, baseItem) }
				?: DreamContent.Clock
		}
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DreamContent.Clock)
}
