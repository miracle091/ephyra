package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.EphyraPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.koin.compose.koinInject

/** Ephyra: "Spotlight" in Customization, Home. */
@Composable
fun HomeSpotlightListButton() {
	val preferences = koinInject<EphyraPreferences>()
	var homeSpotlight by rememberPreference(preferences, EphyraPreferences.homeSpotlight)

	ListButton(
		headingContent = { Text(stringResource(R.string.ephyra_pref_home_spotlight)) },
		captionContent = { Text(stringResource(R.string.ephyra_pref_home_spotlight_description)) },
		trailingContent = { Checkbox(checked = homeSpotlight) },
		onClick = { homeSpotlight = !homeSpotlight },
		modifier = Modifier.focusKey("ephyra_home_spotlight"),
	)
}

/** Ephyra: "Blur unwatched episodes" in Customization. */
@Composable
fun SpoilerBlurListButton() {
	val preferences = koinInject<EphyraPreferences>()
	var spoilerBlur by rememberPreference(preferences, EphyraPreferences.spoilerBlur)

	ListButton(
		headingContent = { Text(stringResource(R.string.ephyra_pref_spoiler_blur)) },
		captionContent = { Text(stringResource(R.string.ephyra_pref_spoiler_blur_description)) },
		trailingContent = { Checkbox(checked = spoilerBlur) },
		onClick = { spoilerBlur = !spoilerBlur },
		modifier = Modifier.focusKey("ephyra_spoiler_blur"),
	)
}

/** Ephyra: "Stop at the end of the season" in Playback, Next up. */
@Composable
fun StopAtSeasonEndListButton() {
	val preferences = koinInject<EphyraPreferences>()
	var stopAtSeasonEnd by rememberPreference(preferences, EphyraPreferences.stopAtSeasonEnd)

	ListButton(
		headingContent = { Text(stringResource(R.string.ephyra_pref_stop_at_season_end)) },
		captionContent = { Text(stringResource(R.string.ephyra_pref_stop_at_season_end_description)) },
		trailingContent = { Checkbox(checked = stopAtSeasonEnd) },
		onClick = { stopAtSeasonEnd = !stopAtSeasonEnd },
		modifier = Modifier.focusKey("ephyra_stop_at_season_end"),
	)
}
