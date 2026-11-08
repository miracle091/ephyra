package org.jellyfin.androidtv.ui.settings.composable

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.dialog.DialogBase
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListControlDefaults
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.ProvideRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.routes
import org.jellyfin.design.Tokens

private data class SettingsSection(
	val route: String,
	@DrawableRes val icon: Int,
	@StringRes val label: Int,
)

/** The top-level entries of the settings, shown in the left pane. Each one is the root route of the right pane. */
private val sections = listOf(
	SettingsSection(Routes.AUTHENTICATION, R.drawable.ic_users, R.string.pref_login),
	SettingsSection(Routes.CUSTOMIZATION, R.drawable.ic_adjust, R.string.pref_customization),
	SettingsSection(Routes.PLAYBACK, R.drawable.ic_next, R.string.pref_playback),
	SettingsSection(Routes.CUSTOMIZATION_SUBTITLES, R.drawable.ic_subtitles, R.string.pref_subtitles),
	SettingsSection(Routes.HOME, R.drawable.ic_house, R.string.home_prefs),
	SettingsSection(Routes.CUSTOMIZATION_SCREENSAVER, R.drawable.ic_photos, R.string.pref_screensaver),
	SettingsSection(Routes.TELEMETRY, R.drawable.ic_error, R.string.pref_telemetry_category),
	SettingsSection(Routes.ABOUT, R.drawable.ic_jellyfin, R.string.pref_about_title),
)

/**
 * Full screen settings with two panes: the sections on the left, the selected section on the right. Moving focus over the left list
 * shows that section, DPAD_RIGHT or enter moves into it and back (BACK) leaves the right pane one level at a time, then returns to the
 * list, then closes the settings. Each section has its own router, so sub-screens open in the right pane and the left pane stays.
 */
@Composable
fun SettingsTwoPaneDialog(
	visible: Boolean,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
) {
	DialogBase(
		visible = visible,
		onDismissRequest = onDismissRequest,
		modifier = modifier,
	) {
		SettingsTwoPane()
	}
}

@Composable
private fun BoxScope.SettingsTwoPane() {
	val palette = LocalEphyraPalette.current
	var selected by remember { mutableStateOf(sections.first().route) }
	val leftPane = remember { FocusRequester() }
	val rightPane = remember { FocusRequester() }
	var rightHasFocus by remember { mutableStateOf(false) }

	Row(
		modifier = Modifier
			.fillMaxSize()
			.background(palette.bg),
	) {
		// Left pane: title and sections
		Column(
			modifier = Modifier
				.width(300.dp)
				.fillMaxHeight()
				.padding(start = 32.dp, top = 27.dp, end = 24.dp, bottom = 27.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			Text(
				text = stringResource(R.string.settings),
				color = palette.ink,
				fontSize = 28.sp,
				lineHeight = 34.sp,
				fontWeight = FontWeight.Bold,
			)

			LazyColumn(
				modifier = Modifier
					// The rows have 12dp of inner padding, shift them so their text lines up with the title
					.offset(x = (-12).dp)
					.focusRequester(leftPane)
					.focusProperties { right = rightPane }
					.focusRestorer(),
				verticalArrangement = Arrangement.spacedBy(Tokens.Space.space2xs),
				contentPadding = PaddingValues(vertical = 4.dp),
			) {
				items(sections, key = { it.route }) { section ->
					SectionButton(
						section = section,
						selected = section.route == selected,
						onFocused = { selected = section.route },
						onClick = { rightPane.requestFocus() },
					)
				}
			}
		}

		// Right pane: the selected section, with its own router for the sub-screens
		Box(
			modifier = Modifier
				.weight(1f)
				.fillMaxHeight()
				.background(palette.surface1)
				.clipToBounds()
				// SettingsColumn already pads its content with the medium space token
				.padding(start = 8.dp, top = 11.dp, end = 16.dp, bottom = 11.dp)
				.focusRequester(rightPane)
				.onFocusChanged { rightHasFocus = it.hasFocus }
				.focusRestorer(),
		) {
			key(selected) {
				ProvideRouter(routes, selected) {
					val router = LocalRouter.current
					// BACK on the root of a section returns to the list, deeper screens are popped by the router
					BackHandler(enabled = rightHasFocus && router.backStack.size <= 1) {
						leftPane.requestFocus()
					}

					SettingsRouterContent()
				}
			}
		}
	}
}

@Composable
private fun SectionButton(
	section: SettingsSection,
	selected: Boolean,
	onFocused: () -> Unit,
	onClick: () -> Unit,
) {
	val palette = LocalEphyraPalette.current
	val interactionSource = remember { MutableInteractionSource() }

	ListButton(
		onClick = onClick,
		interactionSource = interactionSource,
		colors = ListControlDefaults.colors(
			// The open section stays marked while focus is in the right pane
			containerColor = if (selected) palette.surface1 else Color.Transparent,
			focusedContainerColor = palette.surface3,
		),
		leadingContent = { Icon(painterResource(section.icon), contentDescription = null) },
		headingContent = { Text(stringResource(section.label), fontWeight = FontWeight.Bold, fontSize = 14.sp) },
		modifier = Modifier
			.onFocusChanged { if (it.isFocused) onFocused() }
			.semantics { this.selected = selected },
	)
}
