package org.jellyfin.androidtv.ui.playback

import android.widget.TextView
import androidx.core.view.isVisible
import org.jellyfin.androidtv.util.sdk.getSeasonEpisodeName
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

/**
 * Ephyra: the video player's header after the design system's "Player" board, in text instead of the title logo: the series
 * and the episode number (the genres for anything else) as the overline over the episode's or film's name.
 */
object OsdHeader {
	private const val SEPARATOR = " · "

	@JvmStatic
	fun bind(item: BaseItemDto, overline: TextView, title: TextView) {
		val overlineText = overlineOf(item, item.getSeasonEpisodeName(overline.context))
		overline.text = overlineText
		overline.isVisible = overlineText.isNotBlank()
		title.text = item.name
	}

	fun overlineOf(item: BaseItemDto, seasonEpisode: String): String =
		if (item.type == BaseItemKind.EPISODE) {
			listOfNotNull(item.seriesName, seasonEpisode).filter { it.isNotBlank() }.joinToString(SEPARATOR)
		} else {
			item.genres.orEmpty().joinToString(SEPARATOR)
		}
}
