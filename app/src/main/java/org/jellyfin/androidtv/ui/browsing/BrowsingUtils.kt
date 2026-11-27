@file:JvmName("BrowsingUtils")
@file:JvmMultifileClass

package org.jellyfin.androidtv.ui.browsing

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.request.GetAlbumArtistsRequest
import org.jellyfin.sdk.model.api.request.GetArtistsRequest
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetSeasonsRequest
import org.jellyfin.sdk.model.api.request.GetUpcomingEpisodesRequest
import timber.log.Timber
import java.util.UUID

fun getRandomItem(
	api: ApiClient,
	lifecycle: LifecycleOwner,
	library: BaseItemDto,
	type: BaseItemKind,
	callback: (item: BaseItemDto?) -> Unit
) {
	lifecycle.lifecycleScope.launch(Dispatchers.IO) {
		try {
			val result by api.libraryApi.getItems(
				parentId = library.id,
				includeItemTypes = setOf(type),
				recursive = true,
				sortBy = setOf(ItemSortBy.RANDOM),
				limit = 1,
			)

			withContext(Dispatchers.Main) {
				callback(result.items.firstOrNull())
			}
		} catch (error: ApiClientException) {
			Timber.w(error, "Failed to retrieve random item")

			withContext(Dispatchers.Main) {
				callback(null)
			}
		}
	}
}

fun getRandomItems(
	api: ApiClient,
	lifecycle: LifecycleOwner,
	library: BaseItemDto,
	type: BaseItemKind,
	callback: (item: List<BaseItemDto>) -> Unit
) {
	lifecycle.lifecycleScope.launch(Dispatchers.IO) {
		try {
			val result by api.libraryApi.getItems(
				parentId = library.id,
				includeItemTypes = setOf(type),
				recursive = true,
				sortBy = setOf(ItemSortBy.RANDOM),
				limit = 50,
			)

			withContext(Dispatchers.Main) {
				callback(result.items)
			}
		} catch (error: ApiClientException) {
			Timber.w(error, "Failed to retrieve random items")

			withContext(Dispatchers.Main) {
				callback(emptyList())
			}
		}
	}
}

fun createGetNextUpRequest(parentId: UUID) = GetNextUpRequest(
	limit = 50,
	parentId = parentId,
	imageTypeLimit = 1,
	fields = ItemRepository.itemFields
)

@JvmOverloads
fun createLatestMediaRequest(
	parentId: UUID,
	itemType: BaseItemKind? = null,
	groupItems: Boolean? = null
) = GetLatestMediaRequest(
	fields = ItemRepository.itemFields,
	parentId = parentId,
	limit = 50,
	imageTypeLimit = 1,
	includeItemTypes = itemType?.let(::setOf),
	groupItems = groupItems,
)

fun createSeasonsRequest(seriesId: UUID) = GetSeasonsRequest(
	seriesId = seriesId,
	fields = ItemRepository.itemFields,
)

fun createUpcomingEpisodesRequest(parentId: UUID) = GetUpcomingEpisodesRequest(
	parentId = parentId,
	fields = ItemRepository.itemFields,
)

fun createAlbumArtistsRequest(parentId: UUID) = GetAlbumArtistsRequest(
	fields = ItemRepository.itemFields,
	parentId = parentId,
)

fun createArtistsRequest(parentId: UUID) = GetArtistsRequest(
	fields = ItemRepository.itemFields,
	parentId = parentId,
)
