package org.jellyfin.androidtv.util.sdk

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.androidtv.ui.playback.QueueStart
import org.jellyfin.androidtv.ui.player.video.isVideoInProgress
import org.jellyfin.androidtv.util.PlaybackHelper
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.showApi
import org.jellyfin.sdk.api.client.extensions.videoApi
import org.jellyfin.sdk.api.client.extensions.instantMixApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.extensions.ticks
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SdkPlaybackHelper(
	private val api: ApiClient,
	private val userPreferences: UserPreferences,
	private val playbackLauncher: PlaybackLauncher,
	private val playbackControllerContainer: PlaybackControllerContainer,
	// Velella: the new player, so a playback started while it plays a video replaces its screen
	private val playbackManager: PlaybackManager,
) : PlaybackHelper {
	companion object {
		const val ITEM_QUERY_LIMIT = 150
	}

	override fun getItemsToPlay(
		context: Context,
		mainItem: BaseItemDto,
		allowIntros: Boolean,
		shuffle: Boolean,
		outerResponse: Response<List<BaseItemDto>>
	) {
		getScope(context).launch {
			runCatching {
				val items = getItems(mainItem, allowIntros, shuffle)
				if (items.isEmpty() && !mainItem.mediaSources.isNullOrEmpty()) listOf(mainItem)
				else items
			}.fold(
				onSuccess = { items -> outerResponse.onResponse(items) },
				onFailure = { exception ->
					when (exception) {
						is Exception -> outerResponse.onError(exception)
						else -> outerResponse.onError(Exception(exception))
					}
				}
			)
		}
	}

	private suspend fun getItems(
		mainItem: BaseItemDto,
		allowIntros: Boolean,
		shuffle: Boolean,
	): List<BaseItemDto> = withContext(Dispatchers.IO) {
		when (mainItem.type) {
			BaseItemKind.EPISODE -> getEpisodeItems(mainItem)
			BaseItemKind.SERIES -> api.getSeriesEpisodes(mainItem, shuffle)
			BaseItemKind.SEASON -> api.getSeasonEpisodes(mainItem, shuffle)
			BaseItemKind.FOLDER -> api.getFolderItems(mainItem, shuffle)
			BaseItemKind.BOX_SET -> api.getBoxSetItems(mainItem, shuffle)
			BaseItemKind.MUSIC_ALBUM -> api.getAlbumTracks(mainItem, shuffle)
			BaseItemKind.MUSIC_ARTIST -> api.getArtistTracks(mainItem, shuffle)
			BaseItemKind.PLAYLIST -> api.getPlaylistItems(mainItem, shuffle)
			BaseItemKind.PROGRAM -> api.getProgramChannel(mainItem)
			BaseItemKind.TV_CHANNEL -> api.getChannelWithCurrentProgram(mainItem)
			else -> getPartsWithIntros(mainItem, allowIntros)
		}
	}

	private suspend fun getEpisodeItems(mainItem: BaseItemDto): List<BaseItemDto> {
		val seriesId = mainItem.seriesId
		return if (userPreferences[UserPreferences.mediaQueuingEnabled] && seriesId != null) {
			applySeasonEnd(getEpisodeQueue(mainItem, seriesId), mainItem) // Velella: optional stop at the end of the season
		} else {
			listOf(mainItem)
		}
	}

	private suspend fun getPartsWithIntros(mainItem: BaseItemDto, allowIntros: Boolean): List<BaseItemDto> {
		val parts = getParts(mainItem)
		val addIntros = allowIntros && userPreferences[UserPreferences.cinemaModeEnabled]

		return if (addIntros) {
			val intros = runCatching { api.libraryApi.getIntros(mainItem.id).content.items }.getOrNull()
				.orEmpty()
				// Force the type to be trailer as the legacy playback UI uses it to determine if it should show the next up screen
				.map { it.copy(type = BaseItemKind.TRAILER) }

			intros + parts
		} else {
			parts
		}
	}

	/**
	 * Velella: the episode queue starting at [mainItem] (adapted from upstream #5932). The episodes endpoint only lists the
	 * primary version of each episode, so when [mainItem] is an alternate version picked by the user it goes back at the
	 * head of the queue, or the primary version would play instead of the picked one.
	 */
	private suspend fun getEpisodeQueue(mainItem: BaseItemDto, seriesId: UUID): List<BaseItemDto> {
		// The alternate version may not be in the episode list, so try the ids of the other versions too
		val startItemIds = buildList {
			add(mainItem.id)
			mainItem.mediaSources.orEmpty().mapNotNullTo(this) { it.id?.toUUIDOrNull() }
		}.distinct()

		for (startItemId in startItemIds) {
			val response by api.showApi.getEpisodes(
				seriesId = seriesId,
				startItemId = startItemId,
				isMissing = false,
				limit = ITEM_QUERY_LIMIT,
				fields = ItemRepository.itemFields
			)

			val episodes = response.items
			val first = episodes.firstOrNull()
			val isSameEpisode = first != null &&
				(first.id == mainItem.id || first.mediaSources.orEmpty().any { it.id?.toUUIDOrNull() == mainItem.id })
			if (isSameEpisode) return listOf(mainItem) + episodes.drop(1)
		}

		return listOf(mainItem)
	}

	private suspend fun getParts(item: BaseItemDto): List<BaseItemDto> = buildList {
		add(item)

		val partCount = item.partCount
		if (partCount != null && partCount > 1) {
			val response by api.videoApi.getAdditionalPart(item.id)
			addAll(response.items)
		}
	}

	override fun retrieveAndPlay(itemId: UUID, shuffle: Boolean, context: Context) {
		getScope(context).launch {
			val resumeSubtractDuration =
				userPreferences[UserPreferences.resumeSubtractDuration].toIntOrNull()?.seconds
					?: Duration.ZERO

			val item = withContext(Dispatchers.IO) {
				val response by api.libraryApi.getItem(itemId)
				response
			}

			val pos = item.userData?.playbackPositionTicks?.ticks?.minus(resumeSubtractDuration) ?: Duration.ZERO

			val allowIntros = pos == Duration.ZERO && item.type == BaseItemKind.MOVIE
			val items = getItems(item, allowIntros, shuffle)

			playbackLauncher.launch(
				context,
				items,
				pos.inWholeMilliseconds.toInt(),
				playbackControllerContainer.playbackController?.hasFragment() == true || playbackManager.isVideoInProgress,
				QueueStart(0, shuffle),
			)
		}
	}

	override fun retrieveAndPlay(itemIds: List<UUID>, shuffle: Boolean, position: Long?, index: Int?, context: Context) {
		getScope(context).launch {
			val resumeSubtractDuration =
				userPreferences[UserPreferences.resumeSubtractDuration].toIntOrNull()?.seconds
					?: Duration.ZERO

			val items = withContext(Dispatchers.IO) {
				val response by api.libraryApi.getItems(
					ids = itemIds,
				)
				response.items
			}

			val pos = position?.ticks ?: items[0].userData?.playbackPositionTicks?.ticks?.minus(
				resumeSubtractDuration
			) ?: Duration.ZERO

			playbackLauncher.launch(
				context,
				items,
				pos.inWholeMilliseconds.toInt(),
				playbackControllerContainer.playbackController?.hasFragment() == true || playbackManager.isVideoInProgress,
				QueueStart(index ?: 0, shuffle),
			)
		}
	}

	override fun playInstantMix(context: Context, item: BaseItemDto) {
		getScope(context).launch {
			val response = withContext(Dispatchers.IO) {
				api.instantMixApi.getInstantMixFromItem(
					itemId = item.id,
					fields = ItemRepository.itemFields
				).content
			}

			val items = response.items
			if (items.isNotEmpty()) {
				playbackLauncher.launch(context, items)
			} else {
				Toast.makeText(context, R.string.msg_no_playable_items, Toast.LENGTH_LONG).show()
			}
		}
	}

	private fun getScope(context: Context? = null) = when (context) {
		is LifecycleOwner -> context.lifecycleScope
		else -> ProcessLifecycleOwner.get().lifecycleScope
	}
}
