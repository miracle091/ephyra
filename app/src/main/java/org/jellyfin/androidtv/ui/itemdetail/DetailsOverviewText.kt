package org.jellyfin.androidtv.ui.itemdetail

import org.jellyfin.sdk.model.api.BaseItemDto

/** Ephyra: the tagline shown under the title of the details screen. */
object DetailsTagline {
	/** The first non-blank tagline, or null when there is none and the line stays hidden. */
	fun of(taglines: List<String>?): String? = taglines?.firstOrNull { it.isNotBlank() }?.trim()
}

/** Ephyra: the text and progress of the "Next up" card of a series. */
object DetailsNextUp {
	private const val PERCENT = 100.0

	/** "S1:E3 · Title", skipping the parts that are missing. */
	fun title(episodeLabel: String?, name: String?): String =
		listOf(episodeLabel, name).filter { !it.isNullOrBlank() }.joinToString(" · ")

	/** The watched fraction (0..1) of a partially watched episode, or null when it is unstarted or finished. */
	fun progress(item: BaseItemDto): Float? {
		val userData = item.userData ?: return null
		val percentage = userData.playedPercentage
			?: item.runTimeTicks?.takeIf { it > 0 }?.let { runtime -> userData.playbackPositionTicks * PERCENT / runtime }
			?: return null

		return if (percentage > 0.0 && percentage < PERCENT) (percentage / PERCENT).toFloat() else null
	}
}
