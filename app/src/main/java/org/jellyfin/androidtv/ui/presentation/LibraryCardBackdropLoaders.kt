package org.jellyfin.androidtv.ui.presentation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber
import java.util.UUID
import kotlin.random.Random

/** Ephyra: the first backdrop among the titles the server returned for a library, or null when none has one. */
fun libraryCardBackdrop(titles: List<BaseItemDto>): JellyfinImage? = titles.firstNotNullOfOrNull { it.itemBackdropImages.firstOrNull() }

/** Ephyra: the picture of the first of [photos] (the server picks them at random) that has one, or null. */
fun photoCardImage(photos: List<BaseItemDto>): JellyfinImage? = photos.firstNotNullOfOrNull { it.itemImages[ImageType.PRIMARY] }

/**
 * Ephyra: a channel logo for the Live TV card: among the [channels] with a logo (their primary image), the one at [pick] (wrapping
 * around), so the card changes from one start of the app to the next.
 */
fun liveTvCardLogo(channels: List<BaseItemDto>, pick: Int): JellyfinImage? {
	val logos = channels.mapNotNull { it.itemImages[ImageType.PRIMARY] }
	if (logos.isEmpty()) return null
	return logos[pick.mod(logos.size)]
}

internal suspend fun loadLibraryCardBackdrop(api: ApiClient, libraryId: UUID): Result<JellyfinImage?> = withContext(Dispatchers.IO) {
	try {
		val titles = api.libraryApi.getItems(
			parentId = libraryId,
			recursive = true,
			imageTypes = setOf(ImageType.BACKDROP),
			sortBy = setOf(ItemSortBy.DATE_CREATED, ItemSortBy.SORT_NAME),
			sortOrder = setOf(SortOrder.DESCENDING, SortOrder.ASCENDING),
			limit = 1,
		).content.items
		Result.success(libraryCardBackdrop(titles))
	} catch (error: CancellationException) {
		throw error
	} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
		Timber.w(error, "Could not load a backdrop for a library card")
		Result.failure(error)
	}
}

internal suspend fun loadPhotoCardImage(api: ApiClient, libraryId: UUID): Result<JellyfinImage?> = withContext(Dispatchers.IO) {
	try {
		val photos = api.libraryApi.getItems(
			parentId = libraryId,
			recursive = true,
			includeItemTypes = setOf(BaseItemKind.PHOTO),
			imageTypes = setOf(ImageType.PRIMARY),
			sortBy = setOf(ItemSortBy.RANDOM),
			limit = 1,
		).content.items
		Result.success(photoCardImage(photos))
	} catch (error: CancellationException) {
		throw error
	} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
		Timber.w(error, "Could not load a photo for a photo library card")
		Result.failure(error)
	}
}

internal suspend fun loadLiveTvCardLogo(api: ApiClient): Result<JellyfinImage?> = withContext(Dispatchers.IO) {
	try {
		val channels = api.liveTvApi.getLiveTvChannels(
			limit = LIVE_TV_CARD_CHANNELS,
			enableImageTypes = setOf(ImageType.PRIMARY),
			imageTypeLimit = 1,
			enableUserData = false,
		).content.items
		Result.success(liveTvCardLogo(channels, Random.nextInt(LIVE_TV_CARD_CHANNELS)))
	} catch (error: CancellationException) {
		throw error
	} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
		Timber.w(error, "Could not load a channel logo for the Live TV card")
		Result.failure(error)
	}
}

// How many channels (in the server's order) the Live TV card picks a logo from
private const val LIVE_TV_CARD_CHANNELS = 50

internal suspend fun loadRecordingFolderIds(api: ApiClient, userId: UUID): Set<UUID>? = withContext(Dispatchers.IO) {
	try {
		api.liveTvApi.getRecordingFolders(userId).content.items.map { it.id }.toSet()
	} catch (error: CancellationException) {
		throw error
	} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
		Timber.w(error, "Could not load the recordings folders")
		null
	}
}
