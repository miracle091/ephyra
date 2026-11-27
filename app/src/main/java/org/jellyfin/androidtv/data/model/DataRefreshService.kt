package org.jellyfin.androidtv.data.model

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.time.Instant
import java.util.UUID

class DataRefreshService {
	var lastDeletedItemId: UUID? = null
	var lastPlayback: Instant? = null
	var lastMoviePlayback: Instant? = null
	var lastTvPlayback: Instant? = null
	var lastLibraryChange: Instant? = null
	var lastFavoriteUpdate: Instant? = null
	var lastPlayedItem: BaseItemDto? = null

	/** Velella: playback of an item of [type] stopped at [now]: the screens showing it fetch its progress and watched state again. */
	fun markPlaybackStopped(type: BaseItemKind?, now: Instant) {
		lastPlayback = now
		when (type) {
			BaseItemKind.MOVIE -> lastMoviePlayback = now
			BaseItemKind.EPISODE -> lastTvPlayback = now
			else -> Unit
		}
	}
}
