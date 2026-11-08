package org.jellyfin.androidtv.integration.dream.model

import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.sdk.model.api.BaseItemDto

sealed interface DreamContent {
	// Ephyra: a minimal clock instead of the logo and the library showcase
	data object Clock : DreamContent
	data class NowPlaying(val entry: QueueEntry, val item: BaseItemDto) : DreamContent
}
