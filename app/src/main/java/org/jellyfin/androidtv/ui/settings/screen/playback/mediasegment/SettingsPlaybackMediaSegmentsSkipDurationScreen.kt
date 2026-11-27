package org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.playback.overlay.SkipAutoHideChoices
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

/** Velella: the label of a skip button duration choice (0 is "until the segment ends"). */
@Composable
fun skipAutoHideLabel(seconds: Int): String =
	if (seconds > 0) stringResource(R.string.velella_skip_auto_hide_seconds, seconds)
	else stringResource(R.string.velella_skip_auto_hide_segment_end)

/** Velella: how long the skip button stays on screen. */
@Composable
fun SettingsPlaybackMediaSegmentsSkipDurationScreen() {
	val router = LocalRouter.current
	val preferences = koinInject<VelellaPreferences>()
	var skipAutoHideSeconds by rememberPreference(preferences, VelellaPreferences.skipAutoHideSeconds)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_media_segments).uppercase()) },
				headingContent = { Text(stringResource(R.string.velella_pref_skip_auto_hide)) },
			)
		}

		items(SkipAutoHideChoices) { seconds ->
			ListButton(
				headingContent = { Text(skipAutoHideLabel(seconds)) },
				slots = ListItemSlots(
					trailingContent = { RadioButton(checked = skipAutoHideSeconds == seconds) },
				),
				onClick = {
					skipAutoHideSeconds = seconds
					router.back()
				},
				modifier = Modifier.focusKey("skip_auto_hide_$seconds", initialFocus = skipAutoHideSeconds == seconds)
			)
		}
	}
}
