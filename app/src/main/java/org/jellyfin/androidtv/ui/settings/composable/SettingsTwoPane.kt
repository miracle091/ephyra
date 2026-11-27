package org.jellyfin.androidtv.ui.settings.composable

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.dialog.DialogBase
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
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

/** The zero-height focus target above the right pane content, and whether it may take focus right now. */
@Immutable
private class EntryAnchor(val requester: FocusRequester, val isOpen: () -> Boolean)

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
	val palette = LocalVelellaPalette.current
	val focusManager = LocalFocusManager.current
	var selected by remember { mutableStateOf(sections.first().route) }
	val sectionRequesters = remember { sections.associate { it.route to FocusRequester() } }
	val entryAnchor = remember { FocusRequester() }
	var entryAnchorOpen by remember { mutableStateOf(false) }
	var rightHasFocus by remember { mutableStateOf(false) }

	// Entering the right pane through its focus group lands on whatever the focus search picks first, which is not always the first item.
	// Instead, focus a zero-height anchor above the content for a moment and search downwards: that is always the topmost item.
	val enterRightPane = {
		entryAnchorOpen = true
		if (entryAnchor.requestFocus()) {
			focusManager.moveFocus(FocusDirection.Down)
		}
		entryAnchorOpen = false
	}

	Row(
		modifier = Modifier
			.fillMaxSize()
			.background(palette.bg),
	) {
		SettingsLeftPane(
			selected = selected,
			sectionRequesters = sectionRequesters,
			onSelected = { selected = it },
			enterRightPane = enterRightPane,
		)

		SettingsRightPane(
			selected = selected,
			sectionRequesters = sectionRequesters,
			entryAnchor = EntryAnchor(requester = entryAnchor, isOpen = { entryAnchorOpen }),
			rightHasFocus = rightHasFocus,
			onRightFocusChanged = { rightHasFocus = it },
		)
	}
}

/** Left pane: title and sections. */
@Composable
private fun SettingsLeftPane(
	selected: String,
	sectionRequesters: Map<String, FocusRequester>,
	onSelected: (String) -> Unit,
	enterRightPane: () -> Unit,
) {
	val palette = LocalVelellaPalette.current

	Column(
		modifier = Modifier
			.width(SettingsLeftPaneWidth)
			.fillMaxHeight()
			.padding(start = 32.dp, top = 27.dp, end = 24.dp, bottom = 27.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp),
	) {
		Text(
			text = stringResource(R.string.settings),
			style = LocalTextStyle.current.merge(
				color = palette.ink,
				fontSize = 28.sp,
				fontWeight = FontWeight.Bold,
				lineHeight = 34.sp,
			),
		)

		LazyColumn(
			modifier = Modifier
				// The rows have 12dp of inner padding, shift them so their text lines up with the title
				.offset(x = (-12).dp)
				.onPreviewKeyEvent { event ->
					if (event.key == Key.DirectionRight) {
						if (event.type == KeyEventType.KeyDown) enterRightPane()
						true
					} else {
						false
					}
				}
				.focusRestorer(),
			verticalArrangement = Arrangement.spacedBy(Tokens.Space.space2xs),
			contentPadding = PaddingValues(vertical = 4.dp),
		) {
			items(sections, key = { it.route }) { section ->
				SectionButton(
					section = section,
					selected = section.route == selected,
					focusRequester = sectionRequesters.getValue(section.route),
					onFocused = { onSelected(section.route) },
					onClick = enterRightPane,
				)
			}
		}
	}
}

/** Right pane: the selected section, with its own router for the sub-screens. */
@Composable
private fun RowScope.SettingsRightPane(
	selected: String,
	sectionRequesters: Map<String, FocusRequester>,
	entryAnchor: EntryAnchor,
	rightHasFocus: Boolean,
	onRightFocusChanged: (Boolean) -> Unit,
) {
	val palette = LocalVelellaPalette.current

	Box(
		modifier = Modifier
			.weight(1f)
			.fillMaxHeight()
			.background(palette.surface1)
			.clipToBounds()
			.padding(SettingsRightPanePadding)
			.onFocusChanged { onRightFocusChanged(it.hasFocus) }
			.focusGroup(),
	) {
		Spacer(
			Modifier
				.fillMaxWidth()
				.height(0.dp)
				.focusProperties { canFocus = entryAnchor.isOpen() }
				.focusRequester(entryAnchor.requester)
				.focusable()
		)

		key(selected) {
			ProvideRouter(routes, selected) {
				val router = LocalRouter.current
				// BACK on the root of a section returns to the section that is open, deeper screens are popped by the router
				BackHandler(enabled = rightHasFocus && router.backStack.size <= 1) {
					sectionRequesters[selected]?.requestFocus()
				}

				SettingsRouterContent()
			}
		}
	}
}

@Composable
private fun SectionButton(
	section: SettingsSection,
	selected: Boolean,
	focusRequester: FocusRequester,
	onFocused: () -> Unit,
	onClick: () -> Unit,
) {
	val palette = LocalVelellaPalette.current

	ListButton(
		onClick = onClick,
		style = ListControlDefaults.style(
			colors = ListControlDefaults.colors(
				// The open section stays marked while focus is in the right pane
				containerColor = if (selected) palette.surface1 else Color.Transparent,
				focusedContainerColor = palette.surface3,
			),
		),
		slots = ListItemSlots(
			leadingContent = { Icon(painterResource(section.icon), contentDescription = null) },
		),
		headingContent = {
			Text(
				stringResource(section.label),
				style = LocalTextStyle.current.merge(fontSize = 14.sp, fontWeight = FontWeight.Bold),
			)
		},
		modifier = Modifier
			.focusRequester(focusRequester)
			.onFocusChanged { if (it.isFocused) onFocused() }
			.semantics { this.selected = selected },
	)
}
