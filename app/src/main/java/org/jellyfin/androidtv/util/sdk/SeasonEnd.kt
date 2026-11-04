package org.jellyfin.androidtv.util.sdk

import org.jellyfin.androidtv.preference.EphyraPreferences
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.java.KoinJavaComponent

/** Ephyra: the [episodes] of [mainItem]'s season, so autoplay stops when the season ends. Specials in between are left out. */
fun episodesOfSeason(episodes: List<BaseItemDto>, mainItem: BaseItemDto): List<BaseItemDto> {
	val seasonId = mainItem.seasonId ?: return episodes
	return episodes.filter { it.seasonId == seasonId }
}

/** Ephyra: [episodesOfSeason] when the "Stop at the end of the season" setting is on, else [episodes] unchanged. */
fun applySeasonEnd(episodes: List<BaseItemDto>, mainItem: BaseItemDto): List<BaseItemDto> {
	val preferences = KoinJavaComponent.get<EphyraPreferences>(EphyraPreferences::class.java)
	return if (preferences[EphyraPreferences.stopAtSeasonEnd]) episodesOfSeason(episodes, mainItem) else episodes
}
