@file:JvmName("BrowsingUtils")
@file:JvmMultifileClass

package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import java.util.UUID

fun createPersonItemsRequest(personId: UUID, itemType: BaseItemKind) = GetItemsRequest(
	fields = ItemRepository.itemFields,
	personIds = setOf(personId),
	recursive = true,
	includeItemTypes = setOf(itemType),
	sortBy = setOf(ItemSortBy.PREMIERE_DATE, ItemSortBy.SORT_NAME),
	sortOrder = setOf(SortOrder.DESCENDING, SortOrder.ASCENDING),
)

fun createArtistItemsRequest(artistId: UUID, itemType: BaseItemKind) = GetItemsRequest(
	fields = ItemRepository.itemFields,
	artistIds = setOf(artistId),
	recursive = true,
	includeItemTypes = setOf(itemType),
	sortBy = setOf(ItemSortBy.PREMIERE_DATE, ItemSortBy.SORT_NAME),
	sortOrder = setOf(SortOrder.ASCENDING, SortOrder.ASCENDING),
)

fun createNextEpisodesRequest(seasonId: UUID, indexNumber: Int) = GetItemsRequest(
	fields = ItemRepository.itemFields,
	parentId = seasonId,
	includeItemTypes = setOf(BaseItemKind.EPISODE),
	startIndex = indexNumber,
	limit = 20,
)

fun createResumeItemsRequest(parentId: UUID, itemType: BaseItemKind) = GetItemsRequest(
	fields = ItemRepository.itemFields,
	includeItemTypes = setOf(itemType),
	recursive = true,
	parentId = parentId,
	imageTypeLimit = 1,
	limit = 50,
	collapseBoxSetItems = false,
	enableTotalRecordCount = false,
	filters = setOf(ItemFilter.IS_RESUMABLE),
	sortBy = setOf(ItemSortBy.DATE_PLAYED),
	sortOrder = setOf(SortOrder.DESCENDING),
)

fun createFavoriteItemsRequest(parentId: UUID, itemType: BaseItemKind) = GetItemsRequest(
	fields = ItemRepository.itemFields,
	includeItemTypes = setOf(itemType),
	recursive = true,
	parentId = parentId,
	imageTypeLimit = 1,
	filters = setOf(ItemFilter.IS_FAVORITE),
	sortBy = setOf(ItemSortBy.DATE_PLAYED, ItemSortBy.SORT_NAME),
	sortOrder = setOf(SortOrder.DESCENDING, SortOrder.ASCENDING),
)

fun createCollectionsRequest(parentId: UUID) = GetItemsRequest(
	fields = ItemRepository.itemFields,
	includeItemTypes = setOf(BaseItemKind.BOX_SET),
	recursive = true,
	imageTypeLimit = 1,
	parentId = parentId,
	sortBy = setOf(ItemSortBy.SORT_NAME),
)

fun createLastPlayedRequest(parentId: UUID) = GetItemsRequest(
	fields = ItemRepository.itemFields,
	includeItemTypes = setOf(BaseItemKind.AUDIO),
	recursive = true,
	parentId = parentId,
	imageTypeLimit = 1,
	filters = setOf(ItemFilter.IS_PLAYED),
	sortBy = setOf(ItemSortBy.DATE_PLAYED),
	sortOrder = setOf(SortOrder.DESCENDING),
	enableTotalRecordCount = false,
	limit = 50,
)

fun createPlaylistsRequest() = GetItemsRequest(
	fields = ItemRepository.itemFields,
	includeItemTypes = setOf(BaseItemKind.PLAYLIST),
	imageTypeLimit = 1,
	recursive = true,
	sortBy = setOf(ItemSortBy.DATE_CREATED),
	sortOrder = setOf(SortOrder.DESCENDING),
)

fun createBrowseGridItemsRequest(parent: BaseItemDto): GetItemsRequest {
	val baseRequest = GetItemsRequest(
		fields = ItemRepository.gridFields, // Ephyra: lighter pages, see gridFields
		parentId = parent.id,
	)

	if (parent.type == BaseItemKind.USER_VIEW || parent.type == BaseItemKind.COLLECTION_FOLDER) {
		return when (parent.collectionType) {
			CollectionType.MOVIES -> baseRequest.copy(
				includeItemTypes = setOf(BaseItemKind.MOVIE),
				recursive = true,
			)

			CollectionType.TVSHOWS -> baseRequest.copy(
				includeItemTypes = setOf(BaseItemKind.SERIES),
				recursive = true,
			)

			CollectionType.MUSIC -> baseRequest.copy(
				includeItemTypes = setOf(BaseItemKind.MUSIC_ALBUM),
				recursive = true,
			)

			else -> baseRequest
		}
	}

	return baseRequest
}
