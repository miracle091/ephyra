package org.jellyfin.androidtv.ui.settings.screen.about

import android.content.ClipData
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.EphyraInfo
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
import org.jellyfin.androidtv.ui.settings.util.copyAction

@Composable
fun SettingsAboutScreen(launchedFromLogin: Boolean = false) {
	val router = LocalRouter.current

	SettingsColumn {
		if (launchedFromLogin) item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_login).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_about_title)) },
			)
		} else item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_about_title)) },
			)
		}

		item {
			CopyInfoButton(
				heading = stringResource(R.string.ephyra_app_version),
				caption = "${EphyraInfo.VERSION_PREFIX} ${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}",
				icon = R.drawable.ic_jellyfin,
				focusKey = "version",
			)
		}

		// Ephyra: debug builds have their own application id, so a release could not update them
		if (!BuildConfig.DEBUG) item { AppUpdateListButton() }
		if (!BuildConfig.DEBUG) item { DailyUpdateCheckListButton() }

		item {
			CopyInfoButton(
				heading = stringResource(R.string.pref_device_model),
				caption = "${Build.MANUFACTURER} ${Build.MODEL}",
				icon = R.drawable.ic_tv,
				focusKey = "device_model",
			)
		}

		item {
			ListButton(
				slots = ListItemSlots(
					leadingContent = { Icon(painterResource(R.drawable.ic_guide), contentDescription = null) },
				),
				headingContent = { Text(stringResource(R.string.licenses_link)) },
				onClick = { router.push(Routes.LICENSES) },
				modifier = Modifier.focusKey(Routes.LICENSES)
			)
		}

		if (!launchedFromLogin) item {
			ListButton(
				slots = ListItemSlots(
					leadingContent = { Icon(painterResource(R.drawable.ic_flask), contentDescription = null) },
				),
				headingContent = { Text(stringResource(R.string.pref_developer_link)) },
				onClick = { router.push(Routes.DEVELOPER) },
				modifier = Modifier.focusKey(Routes.DEVELOPER)
			)
		}
	}
}

/** A list button that shows [caption] under [heading] and copies it to the clipboard when clicked. */
@Composable
private fun CopyInfoButton(heading: String, caption: String, @DrawableRes icon: Int, focusKey: String) {
	ListButton(
		headingContent = { Text(heading) },
		slots = ListItemSlots(
			leadingContent = { Icon(painterResource(icon), contentDescription = null) },
			captionContent = { Text(caption) },
		),
		onClick = copyAction(ClipData.newPlainText(heading, caption)),
		modifier = Modifier.focusKey(focusKey)
	)
}
