package org.jellyfin.androidtv.ui.settings.screen.library

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.preference.LibraryPreferences
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
import org.koin.compose.koinInject
import java.util.UUID

@Composable
fun SettingsLibrariesDisplayScreen(itemId: UUID, displayPreferencesId: String) {
	val userViewsRepository = koinInject<UserViewsRepository>()
	val userView = rememberUserView(itemId)
	val libraryPreferences = rememberLibraryPreferences(displayPreferencesId) ?: return

	val allowViewSelection = userViewsRepository.allowViewSelection(userView?.collectionType)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_libraries).uppercase()) },
				headingContent = { Text(userView?.name.orEmpty()) },
			)
		}

		item {
			val posterSize by rememberPreference(libraryPreferences, LibraryPreferences.posterSize)

			LibraryDisplayButton(
				heading = R.string.lbl_image_size,
				caption = posterSize.nameRes,
				route = Routes.LIBRARIES_DISPLAY_IMAGE_SIZE,
				itemId = itemId,
				displayPreferencesId = displayPreferencesId,
			)
		}

		item {
			val imageType by rememberPreference(libraryPreferences, LibraryPreferences.imageType)

			LibraryDisplayButton(
				heading = R.string.lbl_image_type,
				caption = imageType.nameRes,
				route = Routes.LIBRARIES_DISPLAY_IMAGE_TYPE,
				itemId = itemId,
				displayPreferencesId = displayPreferencesId,
			)
		}

		item {
			val gridDirection by rememberPreference(libraryPreferences, LibraryPreferences.gridDirection)

			LibraryDisplayButton(
				heading = R.string.grid_direction,
				caption = gridDirection.nameRes,
				route = Routes.LIBRARIES_DISPLAY_GRID,
				itemId = itemId,
				displayPreferencesId = displayPreferencesId,
			)
		}

		if (allowViewSelection) item {
			var enableSmartScreen by rememberPreference(libraryPreferences, LibraryPreferences.enableSmartScreen)

			ListButton(
				headingContent = { Text(stringResource(R.string.enable_smart_view)) },
				slots = ListItemSlots(
					trailingContent = { Checkbox(checked = enableSmartScreen) },
					captionContent = { Text(stringResource(R.string.enable_smart_view_description)) },
				),
				onClick = { enableSmartScreen = !enableSmartScreen },
				modifier = Modifier.focusKey("enable_smart_view")
			)
		}
	}
}

/** A button that shows the current value of a library display preference and opens the screen that changes it. */
@Composable
private fun LibraryDisplayButton(
	@StringRes heading: Int,
	@StringRes caption: Int,
	route: String,
	itemId: UUID,
	displayPreferencesId: String,
) {
	val router = LocalRouter.current

	ListButton(
		headingContent = { Text(stringResource(heading)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(caption)) },
		),
		onClick = {
			router.push(
				route,
				mapOf("itemId" to itemId.toString(), "displayPreferencesId" to displayPreferencesId)
			)
		},
		modifier = Modifier.focusKey(route)
	)
}
