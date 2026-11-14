package org.jellyfin.androidtv.integration

import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.TvContractCompat.WatchNextPrograms
import androidx.tvprovider.media.tv.WatchNextProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.showApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.extensions.ticks
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration

private const val LATEST_MEDIA_LIMIT = 50
private const val RESUME_LIMIT = 10

// Item queries and program mapping used by [LeanbackChannelWorker].

/**
 * Gets the resume and next up episodes. The returned pair contains two lists:
 * 1. resume items
 * 2. next up items
 */
internal suspend fun getNextUpItems(api: ApiClient): Pair<List<BaseItemDto>, List<BaseItemDto>> =
	withContext(Dispatchers.IO) {
		val resume = async {
			api.libraryApi.getResumeItems(
				fields = ItemRepository.itemFields,
				imageTypeLimit = 1,
				limit = RESUME_LIMIT,
				mediaTypes = listOf(MediaType.VIDEO),
				includeItemTypes = listOf(BaseItemKind.EPISODE, BaseItemKind.MOVIE),
				excludeActiveSessions = true,
			).content.items
		}

		val nextUp = async {
			api.showApi.getNextUp(
				imageTypeLimit = 1,
				limit = RESUME_LIMIT,
				enableResumable = false,
				fields = ItemRepository.itemFields,
			).content.items
		}

		// Concat
		Pair(resume.await(), nextUp.await())
	}

internal suspend fun getLatestMedia(api: ApiClient): Triple<List<BaseItemDto>, List<BaseItemDto>, List<BaseItemDto>> =
	withContext(Dispatchers.IO) {
		val latestEpisodes = async {
			api.libraryApi.getLatestMedia(
				fields = ItemRepository.itemFields,
				limit = LATEST_MEDIA_LIMIT,
				includeItemTypes = listOf(BaseItemKind.EPISODE),
				isPlayed = false
			).content
		}

		val latestMovies = async {
			api.libraryApi.getLatestMedia(
				fields = ItemRepository.itemFields,
				limit = LATEST_MEDIA_LIMIT,
				includeItemTypes = listOf(BaseItemKind.MOVIE),
				isPlayed = false
			).content
		}

		val latestMedia = async {
			api.libraryApi.getLatestMedia(
				fields = ItemRepository.itemFields,
				limit = LATEST_MEDIA_LIMIT,
				includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
				isPlayed = false
			).content
		}

		// Concat
		Triple(latestEpisodes.await(), latestMovies.await(), latestMedia.await())
	}

internal fun BaseItemDto.getEpisodeString(): String = when {
	indexNumberEnd != null && indexNumber != null -> "$indexNumber-$indexNumberEnd"
	else -> indexNumber?.toString().orEmpty()
}

internal fun BaseItemDto.getProgramType(): Int = when (type) {
	BaseItemKind.SERIES -> WatchNextPrograms.TYPE_TV_SERIES
	BaseItemKind.MOVIE -> WatchNextPrograms.TYPE_MOVIE
	BaseItemKind.EPISODE -> WatchNextPrograms.TYPE_TV_EPISODE
	BaseItemKind.AUDIO -> WatchNextPrograms.TYPE_TRACK
	BaseItemKind.PLAYLIST -> WatchNextPrograms.TYPE_PLAYLIST
	else -> WatchNextPrograms.TYPE_CHANNEL
}

internal fun BaseItemDto.getPosterAspectRatio(): Int = when (type) {
	BaseItemKind.COLLECTION_FOLDER,
	BaseItemKind.EPISODE -> TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9

	else -> TvContractCompat.PreviewPrograms.ASPECT_RATIO_MOVIE_POSTER
}

/**
 * If we are resuming, we need to show remaining time, cause GoogleTV ignores setLastPlaybackPositionMillis.
 */
internal fun BaseItemDto.getRemainingDurationMillis(): Int {
	val duration = runTimeTicks?.ticks ?: return 0
	val playbackPosition = userData?.playbackPositionTicks?.ticks ?: Duration.ZERO
	return (duration - playbackPosition).inWholeMilliseconds.toInt()
}

@Suppress("RestrictedApi")
internal fun WatchNextProgram.Builder.setTypeAndPosterRatio(item: BaseItemDto) {
	// Poster size & type
	if (item.type == BaseItemKind.EPISODE) {
		setType(WatchNextPrograms.TYPE_TV_EPISODE)
		setPosterArtAspectRatio(WatchNextPrograms.ASPECT_RATIO_16_9)
	} else if (item.type == BaseItemKind.MOVIE) {
		setType(WatchNextPrograms.TYPE_MOVIE)
		setPosterArtAspectRatio(WatchNextPrograms.ASPECT_RATIO_MOVIE_POSTER)
	}
}

@Suppress("RestrictedApi")
internal fun WatchNextProgram.Builder.setWatchNextState(item: BaseItemDto) {
	when {
		// User has started playing the episode
		(item.userData?.playbackPositionTicks ?: 0) > 0 -> {
			setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
			setLastPlaybackPositionMillis(item.userData!!.playbackPositionTicks.ticks.inWholeMilliseconds.toInt())
			// Use last played date to prioritize

			setLastEngagementTimeUtcMillis(
				item.userData?.lastPlayedDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
					?: Instant.now().toEpochMilli()
			)
		}
		// First episode of the season
		item.indexNumber == 1 -> {
			setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_NEW)
			setLastEngagementTimeUtcMillis(
				item.dateCreated?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
					?: Instant.now().toEpochMilli()
			)
		}
		// Default
		else -> {
			setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_NEXT)
			setLastEngagementTimeUtcMillis(Instant.now().toEpochMilli())
		}
	}
}
