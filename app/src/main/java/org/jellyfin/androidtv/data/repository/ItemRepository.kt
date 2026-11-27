package org.jellyfin.androidtv.data.repository

import org.jellyfin.sdk.model.api.ItemFields

object ItemRepository {
	val itemFields = setOf(
		ItemFields.CAN_DELETE,
		ItemFields.CHANNEL_INFO,
		ItemFields.CHAPTERS,
		ItemFields.CHILD_COUNT,
		ItemFields.CUMULATIVE_RUN_TIME_TICKS,
		ItemFields.DATE_CREATED,
		ItemFields.DISPLAY_PREFERENCES_ID,
		ItemFields.GENRES,
		ItemFields.ITEM_COUNTS,
		ItemFields.MEDIA_SOURCE_COUNT,
		ItemFields.MEDIA_SOURCES,
		ItemFields.MEDIA_STREAMS,
		ItemFields.OVERVIEW,
		ItemFields.PATH,
		ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
		ItemFields.TAGLINES,
		ItemFields.TRICKPLAY,
	)

	// Lighter field set for home screen rows - excludes heavy fields like
	// MediaSources, MediaStreams, Chapters, Trickplay that aren't needed for display.
	// Full item data is fetched when user selects an item.
	val browseFields = setOf(
		ItemFields.CAN_DELETE,
		ItemFields.CHILD_COUNT,
		ItemFields.DATE_CREATED,
		ItemFields.GENRES,
		ItemFields.OVERVIEW,
		ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
	)

	/**
	 * Velella: the fields of the library grid's pages: what the cards and the focused item's info row show (media sources
	 * for the resolution and codec badges, counts and run times). Chapters, trickplay, media streams, paths, overviews and
	 * taglines are left out: they make most of the response and the grid never shows them; the details screen and
	 * playback fetch the full item.
	 */
	val gridFields = setOf(
		ItemFields.CAN_DELETE,
		ItemFields.CHANNEL_INFO,
		ItemFields.CHILD_COUNT,
		ItemFields.CUMULATIVE_RUN_TIME_TICKS,
		ItemFields.DATE_CREATED,
		ItemFields.DISPLAY_PREFERENCES_ID,
		ItemFields.GENRES,
		ItemFields.ITEM_COUNTS,
		ItemFields.MEDIA_SOURCE_COUNT,
		ItemFields.MEDIA_SOURCES,
		ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
	)
}
