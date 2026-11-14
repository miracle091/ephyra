package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.BitstreamAudioFormat
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
import org.jellyfin.androidtv.util.AndroidVersion
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackCodecScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_advanced).uppercase()) },
				headingContent = { Text(stringResource(R.string.preference_codecs)) },
				captionContent = { Text(stringResource(R.string.preference_codecs_summary)) },
			)
		}

		item {
			val userAVCLevel by rememberPreference(userPreferences, UserPreferences.userAVCLevel)
			CodecLevelButton(R.string.user_avc_level, userAVCLevel.nameRes, Routes.PLAYBACK_AVC_LEVEL)
		}

		item {
			val userHEVCLevel by rememberPreference(userPreferences, UserPreferences.userHEVCLevel)
			CodecLevelButton(R.string.user_hevc_level, userHEVCLevel.nameRes, Routes.PLAYBACK_HEVC_LEVEL)
		}

		items(BitstreamAudioFormat.entries.size) { index ->
			val format = BitstreamAudioFormat.entries[index]
			val mode = userPreferences[format.preference]

			ListButton(
				headingContent = { Text(stringResource(format.nameRes)) },
				slots = ListItemSlots(
					captionContent = { Text(stringResource(mode.nameRes)) },
				),
				onClick = {
					router.push(
						route = Routes.PLAYBACK_BITSTREAM_AUDIO,
						parameters = mapOf("format" to format.name),
					)
				},
				modifier = Modifier.focusKey("bitstream_audio_${format.name}"),
			)
		}

		if (AndroidVersion.isAtLeastQ) {
			item {
				var softwareCodecsEnabled by rememberPreference(userPreferences, UserPreferences.softwareCodecsEnabled)

				ListButton(
					headingContent = { Text(stringResource(R.string.pref_software_codecs_enabled)) },
					slots = ListItemSlots(
						captionContent = { Text(stringResource(R.string.pref_software_codecs_enabled_description)) },
						trailingContent = { Checkbox(checked = softwareCodecsEnabled) },
					),
					onClick = { softwareCodecsEnabled = !softwareCodecsEnabled },
					modifier = Modifier.focusKey("software_codecs_enabled")
				)
			}
		}
	}
}

/** A list button that shows a codec level [value] and opens its [route] to change it. */
@Composable
private fun CodecLevelButton(@StringRes heading: Int, @StringRes value: Int, route: String) {
	val router = LocalRouter.current

	ListButton(
		headingContent = { Text(stringResource(heading)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(value)) },
		),
		onClick = { router.push(route) },
		modifier = Modifier.focusKey(route)
	)
}
