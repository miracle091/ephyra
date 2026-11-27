package org.jellyfin.androidtv.util.sdk

import org.jellyfin.androidtv.trailer.youTubeTrailerIds
import org.jellyfin.sdk.model.api.BaseItemDto

object TrailerUtils {
	/**
	 * Velella: whether the item has a trailer to play: a local one, or a YouTube one played through Invidious
	 * (TrailerPlayerActivity). Nothing is fetched to decide, the network is used only when the trailer is picked; trailers on
	 * other sites are ignored.
	 */
	@JvmStatic
	fun hasPlayableTrailers(item: BaseItemDto): Boolean = (item.localTrailerCount ?: 0) > 0 || item.youTubeTrailerIds().isNotEmpty()
}
