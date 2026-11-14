@file:JvmName("BaseItemExtensionsKt")
@file:JvmMultifileClass

package org.jellyfin.androidtv.util.sdk

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.ChapterItemInfo
import org.jellyfin.androidtv.util.apiclient.chapterImages
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.LocationType
import org.jellyfin.sdk.model.api.PersonKind
import org.jellyfin.sdk.model.api.PlayAccess

fun BaseItemDto.getSeasonEpisodeName(context: Context): String {
	val seasonNumber = when {
		type == BaseItemKind.EPISODE
			&& parentIndexNumber != null
			&& parentIndexNumber != 0 ->
			context.getString(R.string.lbl_season_number, parentIndexNumber)

		else -> null
	}

	val episodeNumber = when {
		type != BaseItemKind.EPISODE -> indexNumber?.toString()
		parentIndexNumber == 0 -> context.getString(R.string.lbl_special)
		else -> indexNumber?.let { start ->
			indexNumberEnd?.let { end -> context.getString(R.string.lbl_episode_range, start, end) }
				?: context.getString(R.string.lbl_episode_number, start)
		}
	}

	return listOfNotNull(seasonNumber, episodeNumber).joinToString(":")
}

fun BaseItemDto.getDisplayName(context: Context): String {
	val nameSeparator = when (type) {
		BaseItemKind.EPISODE -> " — "
		else -> ". "
	}

	return listOfNotNull(getSeasonEpisodeName(context), name)
		.filter { it.isNotEmpty() }
		.joinToString(nameSeparator)
}


fun BaseItemDto?.canPlay() = this != null
	&& playAccess != PlayAccess.NONE
	&& isPlaceHolder != true
	&& (type != BaseItemKind.EPISODE || locationType != LocationType.VIRTUAL)
	&& type != BaseItemKind.PERSON
	&& (isFolder != true || childCount?.takeIf { it > 0 } != null)

fun BaseItemDto.isLiveTv() = type == BaseItemKind.PROGRAM || type == BaseItemKind.LIVE_TV_CHANNEL
fun BaseItemDto.isNew() = isSeries == true && isNews != true && isRepeat != true

fun BaseItemDto.getFirstPerson(searchedType: PersonKind) = people?.firstOrNull { it.type == searchedType }

fun BaseItemDto.buildChapterItems(): List<ChapterItemInfo> {
	val images = chapterImages
	return chapters?.mapIndexed { i, dto ->
		ChapterItemInfo(
			itemId = id,
			name = dto.name,
			startPositionTicks = dto.startPositionTicks,
			image = images[i].takeIf { it.tag.isNotEmpty() },
		)
	}.orEmpty()
}
