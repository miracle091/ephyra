package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.client.extensions.userViewApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType
import timber.log.Timber
import java.util.UUID

/**
 * Velella: [views] without the live TV recordings folders (the server gives them no collection type, like mixed
 * libraries, so only their ids tell them apart): the Live TV screen opens the recordings.
 */
fun withoutRecordingFolders(views: List<BaseItemDto>, recordingFolderIds: Set<UUID>) = views.filter { it.id !in recordingFolderIds }

/** Velella: the ids of the live TV recordings folders, none when the user has no live TV or the server fails */
suspend fun recordingFolderIds(api: ApiClient): Set<UUID> = withContext(Dispatchers.IO) {
	try {
		api.liveTvApi.getRecordingFolders().content.items.map { it.id }.toSet()
	} catch (error: CancellationException) {
		throw error
	} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
		Timber.w(error, "Could not load the live TV recordings folders")
		emptySet()
	}
}

interface UserViewsRepository {
	val views: Flow<Collection<BaseItemDto>>

	fun isSupported(collectionType: CollectionType?): Boolean
	fun isShownInMyMedia(collectionType: CollectionType?): Boolean
	fun allowViewSelection(collectionType: CollectionType?): Boolean
	fun allowGridView(collectionType: CollectionType?): Boolean
}

class UserViewsRepositoryImpl(
	private val api: ApiClient,
	private val velellaPreferences: VelellaPreferences,
) : UserViewsRepository {
	override val views = flow {
		val views by api.userViewApi.getUserViews()
		val filteredViews = views.items
			.filter { isSupported(it.collectionType) }
		emit(filteredViews)
	}.flowOn(Dispatchers.IO)

	override fun isSupported(collectionType: CollectionType?) = collectionType !in unsupportedCollectionTypes
	// Velella: the home's "My media" row leaves out playlists unless the setting (off by default) shows them
	override fun isShownInMyMedia(collectionType: CollectionType?) = isSupported(collectionType) &&
		(collectionType != CollectionType.PLAYLISTS || velellaPreferences[VelellaPreferences.homePlaylists])
	override fun allowViewSelection(collectionType: CollectionType?) = collectionType !in disallowViewSelectionCollectionTypes
	override fun allowGridView(collectionType: CollectionType?) = collectionType !in disallowGridViewCollectionTypes

	private companion object {
		private val unsupportedCollectionTypes = arrayOf(
			CollectionType.BOOKS,
			CollectionType.FOLDERS
		)

		private val disallowViewSelectionCollectionTypes = arrayOf(
			CollectionType.LIVETV,
			// Velella: music opens as a grid like movies and shows, so it gets their options
			CollectionType.PHOTOS,
		)

		private val disallowGridViewCollectionTypes = arrayOf(
			CollectionType.LIVETV,
		)
	}
}
