@file:JvmName("BaseItemExtensionsKt")
@file:JvmMultifileClass

package org.jellyfin.androidtv.util.sdk

import android.content.Context
import androidx.annotation.PluralsRes
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.getQuantityString
import org.jellyfin.androidtv.util.getTimeFormatter
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.LocationType
import java.time.LocalDateTime

fun BaseItemDto.getProgramSubText(context: Context) = buildString {
	// Add the channel name if set
	channelName?.let { append(channelName, " - ") }

	// Add season and episode info if available
	if (parentIndexNumber != null && parentIndexNumber != 0) {
		append(context.getString(R.string.lbl_season_number, parentIndexNumber), " ")
	}

	if (indexNumber != null && indexNumberEnd != null)
		append(context.getString(R.string.lbl_episode_range, indexNumber, indexNumberEnd), " - ")
	else if (indexNumber != null)
		append(context.getString(R.string.lbl_episode_number, indexNumber), " - ")

	// Add the episode title if set
	episodeTitle?.let { append(episodeTitle, " ") }

	// If the start time is on a different day, add the date
	if (startDate?.dayOfYear != LocalDateTime.now().dayOfYear)
		append(TimeUtils.getFriendlyDate(context, startDate), " ")

	// Add the start and end time
	append(
		context.getString(
			R.string.lbl_time_range,
			context.getTimeFormatter().format(startDate),
			context.getTimeFormatter().format(endDate),
		)
	)
}

fun BaseItemDto.getFullName(context: Context): String? = when (type) {
	BaseItemKind.EPISODE -> buildList {
		add(seriesName)

		if (parentIndexNumber == 0) {
			add(context.getString(R.string.episode_name_special))
		} else {
			if (parentIndexNumber != null)
				add(context.getString(R.string.lbl_season_number, parentIndexNumber))

			if (indexNumber != null && indexNumberEnd != null)
				add(context.getString(R.string.lbl_episode_range, indexNumber, indexNumberEnd))
			else if (indexNumber != null)
				add(context.getString(R.string.lbl_episode_number, indexNumber))

		}
	}.filterNot { it.isNullOrBlank() }.joinToString(" ")
	// we actually want the artist name if available
	BaseItemKind.AUDIO,
	BaseItemKind.MUSIC_ALBUM -> listOfNotNull(
		artists?.joinToString(", ") ?: albumArtists?.joinToString(", ") ?: albumArtist,
		name
	).filter { it.isNotEmpty() }.joinToString(" - ")

	else -> name
}

private fun BaseItemDto.getChildCountText(context: Context, @PluralsRes pluralsRes: Int): String =
	childCount?.takeIf { it > 0 }?.let { context.getQuantityString(pluralsRes, it) } ?: ""

fun BaseItemDto.getSubName(context: Context): String? = when (type) {
	BaseItemKind.EPISODE -> when {
		locationType == LocationType.VIRTUAL && name != null && premiereDate != null ->
			context.getString(
				R.string.lbl_name_date,
				name,
				TimeUtils.getFriendlyDate(context, premiereDate)
			)

		else -> name
	}

	BaseItemKind.SEASON -> getChildCountText(context, R.plurals.episodes)
	BaseItemKind.MUSIC_ALBUM -> getChildCountText(context, R.plurals.tracks)

	BaseItemKind.AUDIO -> name
	else -> officialRating
}
