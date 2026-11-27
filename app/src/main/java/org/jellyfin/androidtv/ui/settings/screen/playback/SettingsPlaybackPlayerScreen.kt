package org.jellyfin.androidtv.ui.settings.screen.playback

import android.content.pm.ResolveInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ExternalAppRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.LocalShapes
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListMessage
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.util.componentName
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackPlayerScreen() {
	val userPreferences = koinInject<UserPreferences>()
	val externalAppRepository = koinInject<ExternalAppRepository>()
	val context = LocalContext.current
	val router = LocalRouter.current

	val externalPlayerApps = remember(context) { externalAppRepository.getExternalPlayerApps(context) }
	val currentExternalPlayer = remember(context) { externalAppRepository.getCurrentExternalPlayerApp(context) }

	var playbackRewriteVideoEnabled by rememberPreference(userPreferences, UserPreferences.playbackRewriteVideoEnabled)
	val showNewPlayer = playbackRewriteVideoEnabled || BuildConfig.DEVELOPMENT

	SettingsColumn {
		item { PlayerHeader() }

		item {
			InternalPlayerButton(
				selected = currentExternalPlayer == null && !playbackRewriteVideoEnabled,
				onClick = {
					playbackRewriteVideoEnabled = false
					externalAppRepository.setExternalPlayerapp(null)
					router.back()
				},
			)
		}

		if (showNewPlayer) item {
			NewPlayerButton(
				selected = currentExternalPlayer == null && playbackRewriteVideoEnabled,
				onClick = {
					playbackRewriteVideoEnabled = true
					externalAppRepository.setExternalPlayerapp(null)
					router.back()
				},
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.video_player_external)) }) }

		if (externalPlayerApps.isEmpty()) {
			item {
				ListMessage {
					Text(stringResource(R.string.velella_video_player_external_empty))
				}
			}
		}

		items(externalPlayerApps) { app ->
			ExternalPlayerButton(
				app = app,
				selected = currentExternalPlayer?.componentName == app.activityInfo.componentName,
				onClick = {
					externalAppRepository.setExternalPlayerapp(app.activityInfo)
					router.back()
				},
			)
		}
	}
}

@Composable
private fun PlayerHeader() {
	ListSection(
		overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
		headingContent = { Text(stringResource(R.string.playback_video_player)) },
	)
}

@Composable
private fun InternalPlayerButton(selected: Boolean, onClick: () -> Unit) {
	PlayerOptionButton(
		option = PlayerOption(
			iconModel = R.mipmap.app_icon,
			heading = stringResource(R.string.app_name),
			caption = stringResource(R.string.video_player_internal),
			key = "player_internal",
			selected = selected,
		),
		onClick = onClick,
	)
}

@Composable
private fun NewPlayerButton(selected: Boolean, onClick: () -> Unit) {
	PlayerOptionButton(
		option = PlayerOption(
			iconModel = R.drawable.ic_flask,
			heading = "New video player",
			caption = stringResource(R.string.enable_playback_module_description),
			key = "player_new",
			selected = selected,
		),
		onClick = onClick,
	)
}

@Composable
private fun ExternalPlayerButton(app: ResolveInfo, selected: Boolean, onClick: () -> Unit) {
	val packageManager = LocalContext.current.packageManager
	val icon = remember(app, packageManager) { app.loadIcon(packageManager) }
	val displayName = remember(app, packageManager) { app.loadLabel(packageManager).toString() }

	PlayerOptionButton(
		option = PlayerOption(
			iconModel = icon,
			heading = displayName,
			caption = stringResource(R.string.video_player_external),
			key = "player_external_${app.activityInfo.componentName}",
			selected = selected,
		),
		onClick = onClick,
	)
}

/** What a row of the player list shows: [selected] is both the radio state and the item that takes the initial focus. */
@Immutable
private data class PlayerOption(
	val iconModel: Any?,
	val heading: String,
	val caption: String,
	val key: String,
	val selected: Boolean,
)

@Composable
private fun PlayerOptionButton(option: PlayerOption, onClick: () -> Unit) {
	ListButton(
		headingContent = { Text(option.heading) },
		slots = ListItemSlots(
			leadingContent = {
				Image(
					painter = rememberAsyncImagePainter(option.iconModel),
					contentDescription = null,
					modifier = Modifier
						.size(32.dp)
						.clip(LocalShapes.current.small)
				)
			},
			trailingContent = { RadioButton(checked = option.selected) },
			captionContent = { Text(option.caption) },
		),
		onClick = onClick,
		modifier = Modifier.focusKey(option.key, initialFocus = option.selected)
	)
}
