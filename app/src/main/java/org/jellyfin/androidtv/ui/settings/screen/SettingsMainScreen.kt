package org.jellyfin.androidtv.ui.settings.screen

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn

@Composable
fun SettingsMainScreen() {
	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.app_name).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings)) },
				captionContent = { Text(stringResource(R.string.settings_description)) },
			)
		}

		item { MainButton(R.drawable.ic_users, R.string.pref_login, Routes.AUTHENTICATION) }

		item { MainButton(R.drawable.ic_adjust, R.string.pref_customization, Routes.CUSTOMIZATION) }

		// TODO: Temporarily added to root - should be accessed via customization screen instead
		item { MainButton(R.drawable.ic_photos, R.string.pref_screensaver, Routes.CUSTOMIZATION_SCREENSAVER) }

		item { MainButton(R.drawable.ic_next, R.string.pref_playback, Routes.PLAYBACK) }

		item { MainButton(R.drawable.ic_error, R.string.pref_telemetry_category, Routes.TELEMETRY) }

		item { MainButton(R.drawable.ic_jellyfin, R.string.pref_about_title, Routes.ABOUT) }
	}
}

@Composable
private fun MainButton(@DrawableRes icon: Int, @StringRes label: Int, route: String) {
	val router = LocalRouter.current

	ListButton(
		slots = ListItemSlots(
			leadingContent = { Icon(painterResource(icon), contentDescription = null) },
		),
		headingContent = { Text(stringResource(label)) },
		onClick = { router.push(route) },
		modifier = Modifier.focusKey(route),
	)
}
