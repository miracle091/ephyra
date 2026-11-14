package org.jellyfin.androidtv.util.sdk

import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.client.extensions.showApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType

// Server queries that list the items to play for a container item, used by [SdkPlaybackHelper].

internal suspend fun ApiClient.getSeriesEpisodes(mainItem: BaseItemDto, shuffle: Boolean): List<BaseItemDto> {
	val response by showApi.getEpisodes(
		seriesId = mainItem.id,
		isMissing = false,
		sortBy = if (shuffle) ItemSortBy.RANDOM else ItemSortBy.SORT_NAME,
		limit = SdkPlaybackHelper.ITEM_QUERY_LIMIT,
		fields = ItemRepository.itemFields,
	)
	return response.items
}

internal suspend fun ApiClient.getSeasonEpisodes(mainItem: BaseItemDto, shuffle: Boolean): List<BaseItemDto> {
	val response by showApi.getEpisodes(
		seriesId = requireNotNull(mainItem.seriesId),
		seasonId = mainItem.id,
		isMissing = false,
		sortBy = if (shuffle) ItemSortBy.RANDOM else ItemSortBy.SORT_NAME,
		limit = SdkPlaybackHelper.ITEM_QUERY_LIMIT,
		fields = ItemRepository.itemFields,
	)
	return response.items
}

internal suspend fun ApiClient.getFolderItems(mainItem: BaseItemDto, shuffle: Boolean): List<BaseItemDto> {
	val response by libraryApi.getItems(
		parentId = mainItem.id,
		isMissing = false,
		includeItemTypes = listOf(
			BaseItemKind.EPISODE,
			BaseItemKind.MOVIE,
			BaseItemKind.VIDEO
		),
		sortBy = if (shuffle) listOf(ItemSortBy.RANDOM) else listOf(ItemSortBy.SORT_NAME),
		recursive = true,
		limit = SdkPlaybackHelper.ITEM_QUERY_LIMIT,
		fields = ItemRepository.itemFields
	)

	return response.items
}

internal suspend fun ApiClient.getBoxSetItems(mainItem: BaseItemDto, shuffle: Boolean): List<BaseItemDto> {
	val response by libraryApi.getItems(
		parentId = mainItem.id,
		isMissing = false,
		includeItemTypes = listOf(
			BaseItemKind.EPISODE,
			BaseItemKind.MOVIE,
			BaseItemKind.VIDEO
		),
		sortBy = if (shuffle) listOf(ItemSortBy.RANDOM) else null,
		recursive = true,
		limit = SdkPlaybackHelper.ITEM_QUERY_LIMIT,
		fields = ItemRepository.itemFields
	)

	return response.items
}

internal suspend fun ApiClient.getAlbumTracks(mainItem: BaseItemDto, shuffle: Boolean): List<BaseItemDto> {
	val response by libraryApi.getItems(
		isMissing = false,
		mediaTypes = listOf(MediaType.AUDIO),
		filters = listOf(ItemFilter.IS_NOT_FOLDER),
		sortBy = if (shuffle) listOf(ItemSortBy.RANDOM) else listOf(
			ItemSortBy.ALBUM,
			ItemSortBy.PARENT_INDEX_NUMBER,
			ItemSortBy.INDEX_NUMBER,
			ItemSortBy.SORT_NAME
		),
		recursive = true,
		limit = SdkPlaybackHelper.ITEM_QUERY_LIMIT,
		fields = ItemRepository.itemFields,
		albumIds = listOf(mainItem.id)
	)

	return response.items
}

internal suspend fun ApiClient.getArtistTracks(mainItem: BaseItemDto, shuffle: Boolean): List<BaseItemDto> {
	val response by libraryApi.getItems(
		isMissing = false,
		mediaTypes = listOf(MediaType.AUDIO),
		filters = listOf(ItemFilter.IS_NOT_FOLDER),
		sortBy = if (shuffle) listOf(ItemSortBy.RANDOM) else listOf(
			ItemSortBy.ALBUM,
			ItemSortBy.PARENT_INDEX_NUMBER,
			ItemSortBy.INDEX_NUMBER,
			ItemSortBy.SORT_NAME
		),
		recursive = true,
		limit = SdkPlaybackHelper.ITEM_QUERY_LIMIT,
		fields = ItemRepository.itemFields,
		artistIds = listOf(mainItem.id)
	)

	return response.items
}

internal suspend fun ApiClient.getPlaylistItems(mainItem: BaseItemDto, shuffle: Boolean): List<BaseItemDto> {
	val response by libraryApi.getItems(
		parentId = mainItem.id,
		isMissing = false,
		sortBy = if (shuffle) listOf(ItemSortBy.RANDOM) else null,
		recursive = true,
		limit = SdkPlaybackHelper.ITEM_QUERY_LIMIT,
		fields = ItemRepository.itemFields
	)

	return response.items
}

internal suspend fun ApiClient.getProgramChannel(mainItem: BaseItemDto): List<BaseItemDto> {
	val parentId = requireNotNull(mainItem.parentId)
	val channel by libraryApi.getItem(parentId)
	val channelWithProgramMetadata = channel.copy(
		premiereDate = mainItem.premiereDate,
		endDate = mainItem.endDate,
		officialRating = mainItem.officialRating,
		runTimeTicks = mainItem.runTimeTicks,
	)

	return listOf(channelWithProgramMetadata)
}

internal suspend fun ApiClient.getChannelWithCurrentProgram(mainItem: BaseItemDto): List<BaseItemDto> {
	val channel by liveTvApi.getChannel(mainItem.id)
	val currentProgram = channel.currentProgram
	return if (currentProgram != null) {
		val channelWithCurrentProgramMetadata = channel.copy(
			premiereDate = currentProgram.premiereDate,
			endDate = currentProgram.endDate,
			officialRating = currentProgram.officialRating,
			runTimeTicks = currentProgram.runTimeTicks,
		)
		listOf(channelWithCurrentProgramMetadata)
	} else {
		listOf(channel)
	}
}
