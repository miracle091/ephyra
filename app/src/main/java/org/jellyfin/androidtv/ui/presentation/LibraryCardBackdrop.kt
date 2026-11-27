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
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.AsyncImageSource
import org.jellyfin.androidtv.ui.composable.item.ArtToneCover
import org.jellyfin.androidtv.ui.composable.item.artToneFor
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.koin.compose.koinInject
import java.util.UUID

/** Velella: what a library card shows while its backdrop is looked up, then the backdrop (null when the library has none). */
sealed interface LibraryCardBackdrop {
	data object Loading : LibraryCardBackdrop
	data class Ready(val image: JellyfinImage?) : LibraryCardBackdrop
}

/** Velella: libraries (Movies, Shows, Music...) show a title's backdrop on their card instead of the server's image with the name on it. */
fun isLibraryCard(item: BaseItemDto?): Boolean = item?.type == BaseItemKind.USER_VIEW || item?.type == BaseItemKind.COLLECTION_FOLDER

/** Velella: the Live TV library holds channels, not titles: its card shows a channel's logo instead of the newest title's backdrop. */
fun isLiveTvLibraryCard(item: BaseItemDto?): Boolean = isLibraryCard(item) && item?.collectionType == CollectionType.LIVETV

/** Velella: the icon of a library card without a backdrop: the REC symbol for the server's recordings folders, else [defaultIcon]. */
fun libraryCardIcon(libraryId: UUID, recordingFolderIds: Collection<UUID>, defaultIcon: Int, recordingIcon: Int): Int =
	if (libraryId in recordingFolderIds) recordingIcon else defaultIcon

/** Velella: a photo library has no titles with a backdrop: its card shows one of its photos, a new one at each start of the app. */
fun isPhotoLibraryCard(item: BaseItemDto?): Boolean = isLibraryCard(item) && item?.collectionType == CollectionType.PHOTOS

/**
 * Velella: the key a library's backdrop is kept under. Users of a server share library ids but not what they may see
 * (parental ratings, hidden folders), so the user is part of it.
 */
fun libraryBackdropKey(userId: UUID?, libraryId: UUID): Pair<UUID?, UUID> = userId to libraryId

// Looked up once per user and library while the app runs; only read and written on the main thread
private val libraryBackdrops = HashMap<Pair<UUID?, UUID>, LibraryCardBackdrop.Ready>()

// The server's recordings folders per user, looked up once while the app runs; only read and written on the main thread
private val recordingFolders = HashMap<UUID?, Set<UUID>>()

/**
 * Velella: the ids of the server's live TV recordings folders. The Recordings library is a plain collection folder without a
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

/** Velella: the backdrop for a library card, from the title added most recently to the library. */
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

/** Velella: a library card's picture: the backdrop when there is one, else the card's colour tone; nothing while loading. */
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
					source = AsyncImageSource(
						url = image.getUrl(
							api,
							maxWidth = with(density) { size.width.roundToPx() },
							maxHeight = with(density) { size.height.roundToPx() },
						),
						blurHash = image.blurHash,
					),
					aspectRatio = aspectRatio,
					scaleType = ImageView.ScaleType.CENTER_CROP,
					modifier = Modifier.fillMaxSize(),
				)
			} else {
				// Velella: the icon always shows, so a library without backdrops (Recordings...) is not a bare colour
				ArtToneCover(title, libraryCardIcon(library.id, recordingFolderIds, iconRes, R.drawable.ic_record), alwaysShowIcon = true)
			}
		}
	}
}

/** Velella: a channel logo (usually a transparent picture) whole and centred on the card's colour tone, not cropped to fill the card. */
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
			source = AsyncImageSource(
				url = logo.getUrl(api, maxWidth = with(density) { (size.width * LOGO_SCALE).roundToPx() }),
			),
			scaleType = ImageView.ScaleType.FIT_CENTER,
			modifier = Modifier.fillMaxSize(LOGO_SCALE),
		)
	}
}

// The share of the card the channel logo may fill
private const val LOGO_SCALE = 0.5f
