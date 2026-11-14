package org.jellyfin.androidtv.ui.browsing.composable.inforow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.util.sdk.getProgramSubText
import org.jellyfin.androidtv.util.sdk.isNew
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.extensions.ticks

/**
 * The number of items shown for a box set: movies, else series, else children.
 */
internal fun boxSetItemCount(item: BaseItemDto): Int? = item.movieCount ?: item.seriesCount ?: item.childCount

/**
 * The runtime (when [includeRuntime]), the official rating and the media details that close most info rows.
 */
@Composable
internal fun InfoRowRuntimeRatingMedia(
	item: BaseItemDto,
	mediaSource: MediaSourceInfo?,
	includeRuntime: Boolean,
	runTimeTicks: Long? = item.runTimeTicks,
) {
	if (includeRuntime) runTimeTicks?.ticks?.let { BaseItemInfoRowRuntime(it) }
	item.officialRating?.let { InfoRowParentalRating(it) }
	mediaSource?.let { InfoRowMediaDetails(it) }
}

@Composable
internal fun InfoRowBoxSet(item: BaseItemDto, mediaSource: MediaSourceInfo?, includeRuntime: Boolean) {
	val count = boxSetItemCount(item)
	if (count != null) {
		InfoRowItem(contentDescription = null) {
			Text(pluralStringResource(R.plurals.items, count, count))
		}
	}
	InfoRowRuntimeRatingMedia(item, mediaSource, includeRuntime, item.cumulativeRunTimeTicks ?: item.runTimeTicks)
}

@Composable
private fun InfoRowLiveBadge() {
	// Ephyra: the design system's danger badge, like the player's LIVE badge
	val palette = LocalEphyraPalette.current
	InfoRowItem(
		contentDescription = null,
		colors = palette.danger to palette.onDanger,
	) {
		Box(
			Modifier
				.size(5.dp)
				.background(palette.onDanger, CircleShape)
		)
		Text(stringResource(R.string.lbl_live).uppercase())
	}
}

@Composable
internal fun InfoRowProgram(item: BaseItemDto, mediaSource: MediaSourceInfo?, includeRuntime: Boolean) {
	val context = LocalContext.current
	InfoRowItem(contentDescription = null) {
		Text(item.getProgramSubText(context))
	}

	if (item.isNew()) {
		InfoRowItem(
			contentDescription = null,
			colors = InfoRowColors.Green,
		) {
			Text(stringResource(R.string.lbl_new))
		}
	}

	if (item.isSeries == true && item.isNews != true) InfoRowDefaultBadge(stringResource(R.string.lbl_repeat))
	if (item.isLive == true) InfoRowLiveBadge()

	InfoRowRuntimeRatingMedia(item, mediaSource, includeRuntime)
}

@Composable
internal fun InfoRowMusicArtist(item: BaseItemDto) {
	// TODO: Appears to always be null? Maybe an API issue?
	val albums = item.albumCount ?: item.childCount ?: 0
	if (albums > 0) {
		InfoRowItem(contentDescription = null) {
			Text(pluralStringResource(R.plurals.albums, albums, albums))
		}
	}
}

@Composable
internal fun InfoRowMusicAlbum(item: BaseItemDto) {
	val artist = item.albumArtist ?: item.albumArtists?.joinToString(", ")
	if (artist != null) {
		InfoRowItem(contentDescription = null) {
			Text(artist)
		}
	}
	InfoRowDate(item)
	val songs = item.songCount ?: item.childCount ?: 0
	if (songs > 0) {
		InfoRowItem(contentDescription = null) {
			Text(pluralStringResource(R.plurals.songs, songs, songs))
		}
	}
}

@Composable
internal fun InfoRowPlaylist(item: BaseItemDto, mediaSource: MediaSourceInfo?, includeRuntime: Boolean) {
	val items = item.childCount ?: 0
	if (items > 0) {
		InfoRowItem(contentDescription = null) {
			Text(pluralStringResource(R.plurals.items, items, items))
		}
	}

	InfoRowRuntimeRatingMedia(item, mediaSource, includeRuntime, item.cumulativeRunTimeTicks ?: item.runTimeTicks)
}
