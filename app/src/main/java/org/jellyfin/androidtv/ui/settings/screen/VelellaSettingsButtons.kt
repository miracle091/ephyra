package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.koin.compose.koinInject

/** Velella: "Spotlight" in Customization, Home. */
@Composable
fun HomeSpotlightListButton() {
	val preferences = koinInject<VelellaPreferences>()
	var homeSpotlight by rememberPreference(preferences, VelellaPreferences.homeSpotlight)

	ListButton(
		headingContent = { Text(stringResource(R.string.velella_pref_home_spotlight)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(R.string.velella_pref_home_spotlight_description)) },
			trailingContent = { Checkbox(checked = homeSpotlight) },
		),
		onClick = { homeSpotlight = !homeSpotlight },
		modifier = Modifier.focusKey("velella_home_spotlight"),
	)
}

/** Velella: "Playlists in My media" in Customization, Home. */
@Composable
fun HomePlaylistsListButton() {
	val preferences = koinInject<VelellaPreferences>()
	var homePlaylists by rememberPreference(preferences, VelellaPreferences.homePlaylists)

	ListButton(
		headingContent = { Text(stringResource(R.string.velella_pref_home_playlists)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(R.string.velella_pref_home_playlists_description)) },
			trailingContent = { Checkbox(checked = homePlaylists) },
		),
		onClick = { homePlaylists = !homePlaylists },
		modifier = Modifier.focusKey("velella_home_playlists"),
	)
}

/** Velella: "Blur unwatched episodes" in Customization. */
@Composable
fun SpoilerBlurListButton() {
	val preferences = koinInject<VelellaPreferences>()
	var spoilerBlur by rememberPreference(preferences, VelellaPreferences.spoilerBlur)

	ListButton(
		headingContent = { Text(stringResource(R.string.velella_pref_spoiler_blur)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(R.string.velella_pref_spoiler_blur_description)) },
			trailingContent = { Checkbox(checked = spoilerBlur) },
		),
		onClick = { spoilerBlur = !spoilerBlur },
		modifier = Modifier.focusKey("velella_spoiler_blur"),
	)
}

/** Velella: "Stop at the end of the season" in Playback, Next up. */
@Composable
fun StopAtSeasonEndListButton() {
	val preferences = koinInject<VelellaPreferences>()
	var stopAtSeasonEnd by rememberPreference(preferences, VelellaPreferences.stopAtSeasonEnd)

	ListButton(
		headingContent = { Text(stringResource(R.string.velella_pref_stop_at_season_end)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(R.string.velella_pref_stop_at_season_end_description)) },
			trailingContent = { Checkbox(checked = stopAtSeasonEnd) },
		),
		onClick = { stopAtSeasonEnd = !stopAtSeasonEnd },
		modifier = Modifier.focusKey("velella_stop_at_season_end"),
	)
}

/** Velella: "Compact episode list" in Customization. */
@Composable
fun CompactEpisodesListButton() {
	val preferences = koinInject<VelellaPreferences>()
	var compactEpisodes by rememberPreference(preferences, VelellaPreferences.compactEpisodeList)

	ListButton(
		headingContent = { Text(stringResource(R.string.velella_pref_compact_episodes)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(R.string.velella_pref_compact_episodes_description)) },
			trailingContent = { Checkbox(checked = compactEpisodes) },
		),
		onClick = { compactEpisodes = !compactEpisodes },
		modifier = Modifier.focusKey("velella_compact_episodes"),
	)
}
