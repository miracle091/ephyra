package org.jellyfin.androidtv.ui.home.spotlight

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.showApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber
import java.util.UUID

/** Velella: how many titles the home spotlight takes in turn. */
const val SPOTLIGHT_SIZE = 5

private const val TICKS_PER_MINUTE = 600_000_000L
private const val MINUTES_PER_HOUR = 60

/** Velella: the films and series added most recently that the user has not watched, with a backdrop to show. */
suspend fun loadSpotlightItems(api: ApiClient): List<BaseItemDto> = withContext(Dispatchers.IO) {
	try {
		api.libraryApi.getItems(
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			isPlayed = false,
			imageTypes = setOf(ImageType.BACKDROP),
			// The name breaks ties (items scanned together share their date), so the order stays the same between loads
			sortBy = setOf(ItemSortBy.DATE_CREATED, ItemSortBy.SORT_NAME),
			sortOrder = setOf(SortOrder.DESCENDING, SortOrder.ASCENDING),
			fields = setOf(ItemFields.GENRES, ItemFields.TAGLINES),
			limit = SPOTLIGHT_SIZE,
		).content.items
	} catch (error: CancellationException) {
		throw error
	} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
		Timber.w(error, "Could not load the home spotlight")
		emptyList()
	}
}

/**
 * Velella: what Play starts. For a series, the server's next up episode (the one in progress, else the first not watched), so
 * playback resumes there and the queue goes on from it; when there is none, the series itself (from the first episode).
 */
suspend fun loadSpotlightPlayTarget(api: ApiClient, item: BaseItemDto): UUID {
	if (item.type != BaseItemKind.SERIES) return item.id
	val nextUp = withContext(Dispatchers.IO) {
		try {
			api.showApi.getNextUp(seriesId = item.id, enableResumable = true, limit = 1).content.items.firstOrNull()
		} catch (error: CancellationException) {
			throw error
		} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
			Timber.w(error, "Could not load the next up episode for the home spotlight")
			null
		}
	}
	return spotlightPlayTarget(item, nextUp)
}

/** Velella: the next up episode of a series when there is one, else the item itself. */
fun spotlightPlayTarget(item: BaseItemDto, nextUp: BaseItemDto?): UUID =
	nextUp?.takeIf { item.type == BaseItemKind.SERIES && it.seriesId == item.id }?.id ?: item.id

/** Velella: the line above the title: the spotlight label, then the first genre ("SPOTLIGHT · THRILLER"). */
fun spotlightOverline(label: String, item: BaseItemDto): String =
	listOfNotNull(label, item.genres?.firstOrNull()).joinToString(" · ")

/** Velella: the running time of a film ("2 h 46 min"), null for anything else or when unknown. */
fun spotlightRuntime(item: BaseItemDto, getString: (Int, Array<out Any>) -> String): String? =
	item.runTimeTicks?.takeIf { item.type == BaseItemKind.MOVIE && it > 0 }?.let { ticks ->
		val minutes = (ticks / TICKS_PER_MINUTE).toInt()
		val hours = minutes / MINUTES_PER_HOUR
		if (hours > 0) getString(R.string.velella_runtime_hours_minutes, arrayOf(hours, minutes % MINUTES_PER_HOUR))
		else getString(R.string.velella_runtime_minutes, arrayOf(minutes))
	}

/** Velella: the first non blank tagline, or null so the spotlight shows no line. */
fun spotlightTagline(item: BaseItemDto): String? = item.taglines?.firstOrNull { it.isNotBlank() }?.trim()
