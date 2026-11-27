package org.jellyfin.androidtv.ui.settings.screen.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AccentColor
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.base.paletteFor
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

/** Velella: Settings > Customization > Accent colour, a list of presets next to "Theme default". */
@Composable
fun SettingsCustomizationAccentScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	val velellaPreferences = koinInject<VelellaPreferences>()
	var accentColor by rememberPreference(velellaPreferences, VelellaPreferences.accentColor)
	// "Theme default" shows the accent of the theme in use
	val themeAccent = paletteFor(userPreferences[UserPreferences.appTheme]).accent

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.velella_pref_accent_color)) },
			)
		}

		items(AccentColor.entries) { entry ->
			ListButton(
				headingContent = { Text(stringResource(entry.nameRes)) },
				slots = ListItemSlots(
					leadingContent = {
						Box(
							modifier = Modifier
								.size(24.dp)
								.background(entry.accent?.let { colorResource(it) } ?: themeAccent, CircleShape)
								.border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
						)
					},
					trailingContent = { RadioButton(checked = accentColor == entry) },
				),
				onClick = {
					accentColor = entry
					router.back()
				},
				modifier = Modifier
					.focusKey("accent_color_${entry.name}", initialFocus = accentColor == entry)
			)
		}
	}
}
