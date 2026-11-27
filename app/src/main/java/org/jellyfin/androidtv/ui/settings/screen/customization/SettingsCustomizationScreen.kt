package org.jellyfin.androidtv.ui.settings.screen.customization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.integration.LeanbackChannelWorker
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.screen.CompactEpisodesListButton
import org.jellyfin.androidtv.ui.settings.screen.SpoilerBlurListButton
import org.koin.compose.koinInject

@Composable
fun SettingsCustomizationScreen() {
	val context = LocalContext.current
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	val workManager = koinInject<WorkManager>()
	val leanbackChannelWorkerAvailable = remember(context) { LeanbackChannelWorker.isAvailable(context) }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_customization)) },
			)
		}

		item { ThemeListButton(userPreferences) }
		item { AccentListButton() } // Velella
		item { ClockListButton(userPreferences) }
		item { WatchedIndicatorListButton(userPreferences) }
		item { SeriesThumbnailsListButton(userPreferences) }
		item { SpoilerBlurListButton() } // Velella
		item { CompactEpisodesListButton() } // Velella
		if (leanbackChannelWorkerAvailable) item { TvProviderListButton(userPreferences, workManager) }

		item { ListSection(headingContent = { Text(stringResource(R.string.pref_browsing)) }) }

		item {
			ListButton(
				slots = ListItemSlots(
					leadingContent = { Icon(painterResource(R.drawable.ic_grid), contentDescription = null) },
				),
				headingContent = { Text(stringResource(R.string.pref_libraries)) },
				onClick = { router.push(Routes.LIBRARIES) },
				modifier = Modifier.focusKey(Routes.LIBRARIES)
			)
		}

		item {
			ListButton(
				slots = ListItemSlots(
					leadingContent = { Icon(painterResource(R.drawable.ic_house), contentDescription = null) },
				),
				headingContent = { Text(stringResource(R.string.home_prefs)) },
				onClick = { router.push(Routes.HOME) },
				modifier = Modifier.focusKey(Routes.HOME)
			)
		}
	}
}

@Composable
private fun ThemeListButton(userPreferences: UserPreferences) {
	val router = LocalRouter.current
	var appTheme by rememberPreference(userPreferences, UserPreferences.appTheme)

	ListButton(
		headingContent = { Text(stringResource(R.string.pref_app_theme)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(appTheme.nameRes)) },
		),
		onClick = { router.push(Routes.CUSTOMIZATION_THEME) },
		modifier = Modifier.focusKey(Routes.CUSTOMIZATION_THEME)
	)
}

// Velella: the accent colour, apart from the theme
@Composable
private fun AccentListButton() {
	val router = LocalRouter.current
	var accentColor by rememberPreference(koinInject<VelellaPreferences>(), VelellaPreferences.accentColor)

	ListButton(
		headingContent = { Text(stringResource(R.string.velella_pref_accent_color)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(accentColor.nameRes)) },
		),
		onClick = { router.push(Routes.CUSTOMIZATION_ACCENT) },
		modifier = Modifier.focusKey(Routes.CUSTOMIZATION_ACCENT)
	)
}

@Composable
private fun ClockListButton(userPreferences: UserPreferences) {
	val router = LocalRouter.current
	var clockBehavior by rememberPreference(userPreferences, UserPreferences.clockBehavior)

	ListButton(
		headingContent = { Text(stringResource(R.string.pref_clock_display)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(clockBehavior.nameRes)) },
		),
		onClick = { router.push(Routes.CUSTOMIZATION_CLOCK) },
		modifier = Modifier.focusKey(Routes.CUSTOMIZATION_CLOCK)
	)
}

@Composable
private fun WatchedIndicatorListButton(userPreferences: UserPreferences) {
	val router = LocalRouter.current
	var watchedIndicatorBehavior by rememberPreference(userPreferences, UserPreferences.watchedIndicatorBehavior)

	ListButton(
		headingContent = { Text(stringResource(R.string.pref_watched_indicator)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(watchedIndicatorBehavior.nameRes)) },
		),
		onClick = { router.push(Routes.CUSTOMIZATION_WATCHED_INDICATOR) },
		modifier = Modifier.focusKey(Routes.CUSTOMIZATION_WATCHED_INDICATOR)
	)
}

@Composable
private fun SeriesThumbnailsListButton(userPreferences: UserPreferences) {
	var seriesThumbnailsEnabled by rememberPreference(userPreferences, UserPreferences.seriesThumbnailsEnabled)

	ListButton(
		headingContent = { Text(stringResource(R.string.lbl_use_series_thumbnails)) },
		slots = ListItemSlots(
			trailingContent = { Checkbox(checked = seriesThumbnailsEnabled) },
			captionContent = { Text(stringResource(R.string.lbl_use_series_thumbnails_description)) },
		),
		onClick = { seriesThumbnailsEnabled = !seriesThumbnailsEnabled },
		modifier = Modifier.focusKey("series_thumbnails_enabled")
	)
}

@Composable
private fun TvProviderListButton(userPreferences: UserPreferences, workManager: WorkManager) {
	var tvProviderEnabled by rememberPreference(userPreferences, UserPreferences.tvProviderEnabled)

	ListButton(
		headingContent = { Text(stringResource(R.string.tv_provider_enabled)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(R.string.velella_tv_provider_enabled_description)) },
			trailingContent = { Checkbox(checked = tvProviderEnabled) },
		),
		onClick = {
			tvProviderEnabled = !tvProviderEnabled
			workManager.enqueue(OneTimeWorkRequestBuilder<LeanbackChannelWorker>().build())
		},
		modifier = Modifier.focusKey("leanback_channels_enabled")
	)
}
