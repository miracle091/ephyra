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
 * the danger "LIVE" badge comes first, then the channel's number and name in the overline's place, and the programme on air moves
 * to the title block above the seek bar (bindProgramme).
 */
object OsdHeader {
	private const val SEPARATOR = " · "
	private const val MAX_GENRES = 2

	@JvmStatic
	fun bind(item: BaseItemDto, overline: TextView, title: TextView, liveBadge: TextView) {
		val isLive = item.type == BaseItemKind.TV_CHANNEL
		val overlineText = overlineOf(item, item.getSeasonEpisodeName(overline.context))

		liveBadge.isVisible = isLive
		// The channel line is 16sp bold ink next to the badge; the other overlines are the small muted caps
		overline.setTextAppearance(if (isLive) R.style.TextAppearance_Ephyra_OsdChannel else R.style.TextAppearance_Ephyra_OsdOverline)
		overline.minHeight = if (isLive) overline.resources.getDimensionPixelSize(R.dimen.ephyra_osd_badge_height) else 0
		overline.text = overlineText
		overline.isVisible = overlineText.isNotBlank()
		// The live programme's title block sits above the seek bar (bindProgramme), the header keeps the channel line
		title.isVisible = !isLive
		title.text = titleOf(item)
	}

	/** The live player's title block above the seek bar: the programme's genres and episode title, then its name. */
	@JvmStatic
	fun bindProgramme(channel: BaseItemDto, overline: TextView, title: TextView) {
		val overlineText = programmeOverlineOf(channel)
		overline.text = overlineText
		overline.isVisible = overlineText.isNotBlank()
		title.text = titleOf(channel)
	}

	fun programmeOverlineOf(channel: BaseItemDto): String {
		val program = channel.currentProgram ?: return ""
		return listOfNotNull(program.genres.orEmpty().take(MAX_GENRES).joinToString(", "), program.episodeTitle)
			.filter { it.isNotBlank() }
			.joinToString(SEPARATOR)
	}

	fun titleOf(item: BaseItemDto): String? =
		if (item.type == BaseItemKind.TV_CHANNEL) item.currentProgram?.name ?: item.name else item.name

	fun overlineOf(item: BaseItemDto, seasonEpisode: String): String = when (item.type) {
		BaseItemKind.TV_CHANNEL -> listOfNotNull(item.number, item.name)
		BaseItemKind.EPISODE -> listOfNotNull(item.seriesName, seasonEpisode)
		else -> item.genres.orEmpty()
	}.filter { it.isNotBlank() }.joinToString(SEPARATOR)
}
