package org.jellyfin.androidtv.ui.settings.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.preference.UpdatePreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.update.AppUpdateState
import org.jellyfin.androidtv.update.AppUpdater
import org.koin.compose.koinInject

/** Ephyra: checks GitHub Releases for a newer version and installs it, one step per click. */
@Composable
fun AppUpdateListButton() {
	val appUpdater = koinInject<AppUpdater>()
	val state by appUpdater.state.collectAsState()

	val heading = when (val current = state) {
		AppUpdateState.Idle, AppUpdateState.UpToDate, AppUpdateState.Failed -> stringResource(R.string.ephyra_update_check)
		AppUpdateState.Checking -> stringResource(R.string.ephyra_update_checking)
		is AppUpdateState.Available -> stringResource(R.string.ephyra_update_available, current.update.versionName)
		is AppUpdateState.Installing -> stringResource(R.string.ephyra_update_installing, current.update.versionName)
	}
	val caption = when (state) {
		AppUpdateState.Idle, AppUpdateState.Checking -> stringResource(R.string.ephyra_update_check_description)
		AppUpdateState.UpToDate -> stringResource(R.string.ephyra_update_up_to_date)
		is AppUpdateState.Available, is AppUpdateState.Installing -> stringResource(R.string.ephyra_update_available_description)
		AppUpdateState.Failed -> stringResource(R.string.ephyra_update_failed)
	}

	ListButton(
		leadingContent = { Icon(painterResource(R.drawable.ic_loop), contentDescription = null) },
		headingContent = { Text(heading) },
		captionContent = { Text(caption) },
		// Stays enabled while busy so it keeps the D-pad focus; AppUpdater ignores clicks until it is done
		onClick = {
			when (val current = state) {
				is AppUpdateState.Available -> appUpdater.install(current.update)
				else -> appUpdater.checkForUpdate()
			}
		},
		modifier = Modifier.focusKey("app_update"),
	)
}

/** Ephyra: turns the daily update check (update/DailyUpdateCheck.kt) on or off. */
@Composable
fun DailyUpdateCheckListButton() {
	val updatePreferences = koinInject<UpdatePreferences>()
	var enabled by rememberPreference(updatePreferences, UpdatePreferences.dailyCheckEnabled)

	ListButton(
		headingContent = { Text(stringResource(R.string.ephyra_update_daily_check)) },
		trailingContent = { Checkbox(checked = enabled) },
		captionContent = {
			if (enabled) Text(stringResource(R.string.ephyra_update_daily_check_enabled))
			else Text(stringResource(R.string.ephyra_update_daily_check_disabled))
		},
		onClick = { enabled = !enabled },
		modifier = Modifier.focusKey("daily_update_check"),
	)
}
