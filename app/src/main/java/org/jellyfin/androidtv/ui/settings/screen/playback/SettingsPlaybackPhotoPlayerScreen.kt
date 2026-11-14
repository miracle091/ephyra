package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.base.form.RangeControlState
import org.jellyfin.androidtv.ui.base.list.ListControl
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.design.Tokens
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SettingsPlaybackPhotoPlayerScreen() {
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.photo_player)) },
			)
		}

		item {
			var photoPlayerPresentationDelay by rememberPreference(userPreferences, UserPreferences.photoPlayerPresentationDelay)
			val interactionSource = remember { MutableInteractionSource() }

			ListControl(
				headingContent = { Text(stringResource(R.string.photo_display_duration)) },
				slots = ListItemSlots(
					captionContent = { Text(stringResource(R.string.photo_display_duration_description)) },
					footerContent = {
						Row(
							verticalAlignment = Alignment.CenterVertically,
						) {
							RangeControl(
								modifier = Modifier
									.height(4.dp)
									.weight(1f),
								interactionSource = interactionSource,
								// 4 - 120 seconds with 2 second increment
								state = RangeControlState(
									value = photoPlayerPresentationDelay.toFloat(),
									min = 4_000f,
									max = 120_000f,
									step = 2_000f,
									markers = listOf(UserPreferences.photoPlayerPresentationDelay.defaultValue.toFloat()),
								),
								onValueChange = { photoPlayerPresentationDelay = it.toLong() }
							)

							Spacer(Modifier.width(Tokens.Space.spaceSm))

							Box(
								modifier = Modifier.sizeIn(minWidth = 32.dp),
								contentAlignment = Alignment.CenterEnd
							) {
								Text("${photoPlayerPresentationDelay.milliseconds.inWholeSeconds}s")
							}
						}
					},
				),
				interactionSource = interactionSource,
				modifier = Modifier.focusKey("photo_display_duration")
			)
		}
	}
}
