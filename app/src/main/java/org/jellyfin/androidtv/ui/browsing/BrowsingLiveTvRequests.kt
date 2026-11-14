@file:JvmName("BrowsingUtils")
@file:JvmMultifileClass

package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.model.api.request.GetLiveTvChannelsRequest
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest

fun createLiveTVOnNowRequest() = GetRecommendedProgramsRequest(
	isAiring = true,
	fields = ItemRepository.itemFields,
	imageTypeLimit = 1,
	enableTotalRecordCount = false,
	limit = 150,
)

fun createLiveTVUpcomingRequest() = GetRecommendedProgramsRequest(
	isAiring = false,
	hasAired = false,
	fields = ItemRepository.itemFields,
	imageTypeLimit = 1,
	enableTotalRecordCount = false,
	limit = 150,
)

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
