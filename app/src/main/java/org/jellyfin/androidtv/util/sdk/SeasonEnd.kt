package org.jellyfin.androidtv.util.sdk

import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.java.KoinJavaComponent

/** Velella: the [episodes] of [mainItem]'s season, so autoplay stops when the season ends. Specials in between are left out. */
fun episodesOfSeason(episodes: List<BaseItemDto>, mainItem: BaseItemDto): List<BaseItemDto> {
	val seasonId = mainItem.seasonId ?: return episodes
	return episodes.filter { it.seasonId == seasonId }
}

/** Velella: [episodesOfSeason] when the "Stop at the end of the season" setting is on, else [episodes] unchanged. */
fun applySeasonEnd(episodes: List<BaseItemDto>, mainItem: BaseItemDto): List<BaseItemDto> {
	val preferences = KoinJavaComponent.get<VelellaPreferences>(VelellaPreferences::class.java)
	return if (preferences[VelellaPreferences.stopAtSeasonEnd]) episodesOfSeason(episodes, mainItem) else episodes
}
