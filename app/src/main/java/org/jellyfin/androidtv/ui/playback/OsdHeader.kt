package org.jellyfin.androidtv.ui.playback

import android.widget.TextView
import androidx.core.view.isVisible
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.sdk.getSeasonEpisodeName
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

/**
 * Ephyra: the video player's header after the design system's "Player" board, in text instead of the title logo: the series
 * and the episode number (the genres for anything else) as the overline over the episode's or film's name. For a live channel
 * the overline is "Live", the channel number and the channel's name, and the title is the programme on air.
 */
object OsdHeader {
	private const val SEPARATOR = " · "

	@JvmStatic
	fun bind(item: BaseItemDto, overline: TextView, title: TextView) {
		val overlineText = overlineOf(item, item.getSeasonEpisodeName(overline.context), overline.context.getString(R.string.lbl_live))
		overline.text = overlineText
		overline.isVisible = overlineText.isNotBlank()
		title.text = titleOf(item)
	}

	fun titleOf(item: BaseItemDto): String? =
		if (item.type == BaseItemKind.TV_CHANNEL) item.currentProgram?.name ?: item.name else item.name

	fun overlineOf(item: BaseItemDto, seasonEpisode: String, liveLabel: String = ""): String = when (item.type) {
		BaseItemKind.TV_CHANNEL -> listOfNotNull(liveLabel, item.number, item.name)
		BaseItemKind.EPISODE -> listOfNotNull(item.seriesName, seasonEpisode)
		else -> item.genres.orEmpty()
	}.filter { it.isNotBlank() }.joinToString(SEPARATOR)
}
