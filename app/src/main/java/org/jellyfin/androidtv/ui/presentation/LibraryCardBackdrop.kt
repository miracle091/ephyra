package org.jellyfin.androidtv.ui.presentation

import android.widget.ImageView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.item.ArtToneCover
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.compose.koinInject
import timber.log.Timber
import java.util.UUID

/** Ephyra: what a library card shows while its backdrop is looked up, then the backdrop (null when the library has none). */
sealed interface LibraryCardBackdrop {
	data object Loading : LibraryCardBackdrop
	data class Ready(val image: JellyfinImage?) : LibraryCardBackdrop
}

/** Ephyra: libraries (Movies, Shows, Music...) show a title's backdrop on their card instead of the server's image with the name on it. */
fun isLibraryCard(item: BaseItemDto?): Boolean = item?.type == BaseItemKind.USER_VIEW || item?.type == BaseItemKind.COLLECTION_FOLDER

/** Ephyra: the Live TV library holds channels, not titles with a backdrop, so its card shows the colour tone with a TV icon. */
fun isLiveTvLibraryCard(item: BaseItemDto?): Boolean = isLibraryCard(item) && item?.collectionType == CollectionType.LIVETV

/** Ephyra: the first backdrop among the titles the server returned for a library, or null when none has one. */
fun libraryCardBackdrop(titles: List<BaseItemDto>): JellyfinImage? = titles.firstNotNullOfOrNull { it.itemBackdropImages.firstOrNull() }

/**
 * Ephyra: the key a library's backdrop is kept under. Users of a server share library ids but not what they may see
 * (parental ratings, hidden folders), so the user is part of it.
 */
fun libraryBackdropKey(userId: UUID?, libraryId: UUID): Pair<UUID?, UUID> = userId to libraryId

// Looked up once per user and library while the app runs; only read and written on the main thread
private val libraryBackdrops = HashMap<Pair<UUID?, UUID>, LibraryCardBackdrop.Ready>()

/** Ephyra: the backdrop for a library card, from the title added most recently to the library. */
@Composable
fun rememberLibraryCardBackdrop(library: BaseItemDto): State<LibraryCardBackdrop> {
	val api = koinInject<ApiClient>()
	val user by koinInject<UserRepository>().currentUser.collectAsState()
	val key = libraryBackdropKey(user?.id, library.id)
	return produceState(libraryBackdrops[key] ?: LibraryCardBackdrop.Loading, key) {
		if (key in libraryBackdrops) return@produceState
		// A failed lookup is not cached, so the next bind tries again; meanwhile the card shows its colour tone
		val result = loadLibraryCardBackdrop(api, library.id)
		val backdrop = LibraryCardBackdrop.Ready(result.getOrNull())
		if (result.isSuccess) libraryBackdrops[key] = backdrop
		value = backdrop
	}
}

/** Ephyra: a library card's picture: the backdrop when there is one, else the card's colour tone; nothing while loading. */
@Composable
fun LibraryCardImage(library: BaseItemDto, title: String?, iconRes: Int, size: DpSize, aspectRatio: Float) {
	val backdrop by rememberLibraryCardBackdrop(library)
	when (val state = backdrop) {
		LibraryCardBackdrop.Loading -> Unit
		is LibraryCardBackdrop.Ready -> {
			val image = state.image
			if (image != null) {
				val api = koinInject<ApiClient>()
				val density = LocalDensity.current
				AsyncImage(
					url = image.getUrl(
						api,
						maxWidth = with(density) { size.width.roundToPx() },
						maxHeight = with(density) { size.height.roundToPx() },
					),
					blurHash = image.blurHash,
					aspectRatio = aspectRatio,
					scaleType = ImageView.ScaleType.CENTER_CROP,
					modifier = Modifier.fillMaxSize(),
				)
			} else {
				// Ephyra: the icon always shows, so a library without backdrops (Recordings...) is not a bare colour
				ArtToneCover(title, iconRes, alwaysShowIcon = true)
			}
		}
	}
}

private suspend fun loadLibraryCardBackdrop(api: ApiClient, libraryId: UUID): Result<JellyfinImage?> = withContext(Dispatchers.IO) {
	try {
		val titles = api.itemsApi.getItems(
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
