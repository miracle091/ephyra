package org.jellyfin.androidtv.ui.presentation

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import org.jellyfin.androidtv.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.item.ArtToneCover
import org.jellyfin.androidtv.ui.composable.item.artToneFor
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.compose.koinInject
import timber.log.Timber
import java.util.UUID
import kotlin.random.Random

/** Ephyra: what a library card shows while its backdrop is looked up, then the backdrop (null when the library has none). */
sealed interface LibraryCardBackdrop {
	data object Loading : LibraryCardBackdrop
	data class Ready(val image: JellyfinImage?) : LibraryCardBackdrop
}

/** Ephyra: libraries (Movies, Shows, Music...) show a title's backdrop on their card instead of the server's image with the name on it. */
fun isLibraryCard(item: BaseItemDto?): Boolean = item?.type == BaseItemKind.USER_VIEW || item?.type == BaseItemKind.COLLECTION_FOLDER

/** Ephyra: the Live TV library holds channels, not titles: its card shows a channel's logo instead of the newest title's backdrop. */
fun isLiveTvLibraryCard(item: BaseItemDto?): Boolean = isLibraryCard(item) && item?.collectionType == CollectionType.LIVETV

/** Ephyra: the icon of a library card without a backdrop: the REC symbol for the server's recordings folders, else [defaultIcon]. */
fun libraryCardIcon(libraryId: UUID, recordingFolderIds: Collection<UUID>, defaultIcon: Int, recordingIcon: Int): Int =
	if (libraryId in recordingFolderIds) recordingIcon else defaultIcon

/** Ephyra: the first backdrop among the titles the server returned for a library, or null when none has one. */
fun libraryCardBackdrop(titles: List<BaseItemDto>): JellyfinImage? = titles.firstNotNullOfOrNull { it.itemBackdropImages.firstOrNull() }

/** Ephyra: a photo library has no titles with a backdrop: its card shows one of its photos, a new one at each start of the app. */
fun isPhotoLibraryCard(item: BaseItemDto?): Boolean = isLibraryCard(item) && item?.collectionType == CollectionType.PHOTOS

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

/**
 * Ephyra: the key a library's backdrop is kept under. Users of a server share library ids but not what they may see
 * (parental ratings, hidden folders), so the user is part of it.
 */
fun libraryBackdropKey(userId: UUID?, libraryId: UUID): Pair<UUID?, UUID> = userId to libraryId

// Looked up once per user and library while the app runs; only read and written on the main thread
private val libraryBackdrops = HashMap<Pair<UUID?, UUID>, LibraryCardBackdrop.Ready>()

// The server's recordings folders per user, looked up once while the app runs; only read and written on the main thread
private val recordingFolders = HashMap<UUID?, Set<UUID>>()

/**
 * Ephyra: the ids of the server's live TV recordings folders. The Recordings library is a plain collection folder without a
 * collection type, so only the server can tell it apart from other folders.
 */
@Composable
private fun rememberRecordingFolderIds(): State<Set<UUID>> {
	val api = koinInject<ApiClient>()
	val user by koinInject<UserRepository>().currentUser.collectAsState()
	val userId = user?.id
	return produceState(recordingFolders[userId] ?: emptySet(), userId) {
		if (userId == null || userId in recordingFolders) return@produceState
		val ids = loadRecordingFolderIds(api, userId) ?: return@produceState
		recordingFolders[userId] = ids
		value = ids
	}
}

/** Ephyra: the backdrop for a library card, from the title added most recently to the library. */
@Composable
fun rememberLibraryCardBackdrop(library: BaseItemDto): State<LibraryCardBackdrop> {
	val api = koinInject<ApiClient>()
	val user by koinInject<UserRepository>().currentUser.collectAsState()
	val key = libraryBackdropKey(user?.id, library.id)
	return produceState(libraryBackdrops[key] ?: LibraryCardBackdrop.Loading, key) {
		if (key in libraryBackdrops) return@produceState
		// A failed lookup is not cached, so the next bind tries again; meanwhile the card shows its colour tone
		val result = when {
			isLiveTvLibraryCard(library) -> loadLiveTvCardLogo(api)
			isPhotoLibraryCard(library) -> loadPhotoCardImage(api, library.id)
			else -> loadLibraryCardBackdrop(api, library.id)
		}
		val backdrop = LibraryCardBackdrop.Ready(result.getOrNull())
		if (result.isSuccess) libraryBackdrops[key] = backdrop
		value = backdrop
	}
}

/** Ephyra: a library card's picture: the backdrop when there is one, else the card's colour tone; nothing while loading. */
@Composable
fun LibraryCardImage(library: BaseItemDto, title: String?, iconRes: Int, size: DpSize, aspectRatio: Float) {
	val backdrop by rememberLibraryCardBackdrop(library)
	val recordingFolderIds by rememberRecordingFolderIds()
	when (val state = backdrop) {
		LibraryCardBackdrop.Loading -> Unit
		is LibraryCardBackdrop.Ready -> {
			val image = state.image
			if (image != null && isLiveTvLibraryCard(library)) {
				LiveTvCardLogo(image, title, size)
			} else if (image != null) {
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
				ArtToneCover(title, libraryCardIcon(library.id, recordingFolderIds, iconRes, R.drawable.ic_record), alwaysShowIcon = true)
			}
		}
	}
}

private suspend fun loadLibraryCardBackdrop(api: ApiClient, libraryId: UUID): Result<JellyfinImage?> = withContext(Dispatchers.IO) {
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

/** Ephyra: a channel logo (usually a transparent picture) whole and centred on the card's colour tone, not cropped to fill the card. */
@Composable
private fun LiveTvCardLogo(logo: JellyfinImage, title: String?, size: DpSize) {
	val api = koinInject<ApiClient>()
	val density = LocalDensity.current
	Box(
		contentAlignment = Alignment.Center,
		modifier = Modifier
			.fillMaxSize()
			.background(artToneFor(title)),
	) {
		AsyncImage(
			url = logo.getUrl(api, maxWidth = with(density) { (size.width * LOGO_SCALE).roundToPx() }),
			scaleType = ImageView.ScaleType.FIT_CENTER,
			modifier = Modifier.fillMaxSize(LOGO_SCALE),
		)
	}
}

// The share of the card the channel logo may fill
private const val LOGO_SCALE = 0.5f

private suspend fun loadPhotoCardImage(api: ApiClient, libraryId: UUID): Result<JellyfinImage?> = withContext(Dispatchers.IO) {
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

private suspend fun loadLiveTvCardLogo(api: ApiClient): Result<JellyfinImage?> = withContext(Dispatchers.IO) {
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

private suspend fun loadRecordingFolderIds(api: ApiClient, userId: UUID): Set<UUID>? = withContext(Dispatchers.IO) {
	try {
		api.liveTvApi.getRecordingFolders(userId).content.items.map { it.id }.toSet()
	} catch (error: CancellationException) {
		throw error
	} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
		Timber.w(error, "Could not load the recordings folders")
		null
	}
}
