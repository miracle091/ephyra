@file:JvmName("BrowsingUtils")
@file:JvmMultifileClass

package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.model.api.request.GetLiveTvChannelsRequest
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest

@JvmOverloads
fun createLiveTVRecordingsRequest(limit: Int? = null) = GetRecordingsRequest(
	fields = ItemRepository.itemFields,
	enableImages = true,
	limit = limit,
)

fun createLiveTVMovieRecordingsRequest() = GetRecordingsRequest(
	fields = ItemRepository.itemFields,
	enableImages = true,
	limit = 60,
	isMovie = true,
)

fun createLiveTVSeriesRecordingsRequest() = GetRecordingsRequest(
	fields = ItemRepository.itemFields,
	enableImages = true,
	limit = 60,
	isSeries = true,
)

fun createLiveTVSportsRecordingsRequest() = GetRecordingsRequest(
	fields = ItemRepository.itemFields,
	enableImages = true,
	limit = 60,
	isSports = true,
)

fun createLiveTVKidsRecordingsRequest() = GetRecordingsRequest(
	fields = ItemRepository.itemFields,
	enableImages = true,
	limit = 60,
	isKids = true,
)

fun createLiveTVChannelsRequest(isFavorite: Boolean) = GetLiveTvChannelsRequest(
	isFavorite = isFavorite,
)
