package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ExternalAppRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalShapes
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackScreen() {
	val context = LocalContext.current
	val router = LocalRouter.current
	val externalAppRepository = koinInject<ExternalAppRepository>()
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback)) },
			)
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.playback_video_player)) },
				slots = ListItemSlots(
					leadingContent = { Icon(painterResource(R.drawable.ic_tv_play), contentDescription = null) },
					trailingContent = {
						val iconDrawable = remember(context) {
							externalAppRepository.getCurrentExternalPlayerApp(context)?.loadIcon(context.packageManager)
						}
						Image(
							painter = if (iconDrawable == null) rememberAsyncImagePainter(R.mipmap.app_icon)
							else rememberAsyncImagePainter(iconDrawable),
							contentDescription = null,
							modifier = Modifier
								.size(24.dp)
								.clip(LocalShapes.current.small)
						)
					},
				),
				onClick = { router.push(Routes.PLAYBACK_PLAYER) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_PLAYER)
			)
		}

		item { PlaybackButton(R.drawable.ic_photo, R.string.photo_player, Routes.PLAYBACK_PHOTO_PLAYER) }

		item { PlaybackButton(R.drawable.ic_next_up, R.string.pref_playback_next_up, Routes.PLAYBACK_NEXT_UP) }

		item {
			val stillWatchingBehavior by rememberPreference(userPreferences, UserPreferences.stillWatchingBehavior)
			PlaybackButton(
				icon = R.drawable.ic_zzz,
				heading = R.string.pref_playback_inactivity_prompt,
				route = Routes.PLAYBACK_INACTIVITY_PROMPT,
				caption = stillWatchingBehavior.nameRes,
			)
		}

		item { PlaybackButton(R.drawable.ic_trailer, R.string.pref_playback_prerolls, Routes.PLAYBACK_PREROLLS) }

		item { PlaybackButton(R.drawable.ic_subtitles, R.string.pref_customization_subtitles, Routes.CUSTOMIZATION_SUBTITLES) }

		item { PlaybackButton(R.drawable.ic_clapperboard, R.string.pref_playback_media_segments, Routes.PLAYBACK_MEDIA_SEGMENTS) }

		item { PlaybackButton(R.drawable.ic_more, R.string.pref_playback_advanced, Routes.PLAYBACK_ADVANCED) }
	}
}

@Composable
private fun PlaybackButton(
	@DrawableRes icon: Int,
	@StringRes heading: Int,
	route: String,
	@StringRes caption: Int? = null,
) {
	val router = LocalRouter.current

	ListButton(
		headingContent = { Text(stringResource(heading)) },
		slots = ListItemSlots(
			leadingContent = { Icon(painterResource(icon), contentDescription = null) },
			captionContent = if (caption != null) ({ Text(stringResource(caption)) }) else null,
		),
		onClick = { router.push(route) },
		modifier = Modifier.focusKey(route)
	)
}
