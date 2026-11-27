package org.jellyfin.androidtv.ui.settings.screen.livetv

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.LiveTvPreferences
import org.jellyfin.androidtv.preference.constant.LiveTvChannelOrder
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
import org.jellyfin.preference.Preference
import org.koin.compose.koinInject

@Composable
fun SettingsLiveTvGuideOptionsScreen() {
	val router = LocalRouter.current
	val liveTvPreferences = koinInject<LiveTvPreferences>()

	val indicators = listOf(
		rememberPreference(liveTvPreferences, LiveTvPreferences.showHDIndicator) to stringResource(R.string.lbl_hd_programs),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showLiveIndicator) to stringResource(R.string.lbl_live_broadcasts),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showNewIndicator) to stringResource(R.string.lbl_new_episodes),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showPremiereIndicator) to stringResource(R.string.lbl_premieres),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showRepeatIndicator) to stringResource(R.string.lbl_repeat_episodes),
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.lbl_live_tv_guide).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings)) },
			)
		}

		item { ChannelOrderButton(onClick = { router.push(Routes.LIVETV_GUIDE_CHANNEL_ORDER) }) }

		for (option in guideCheckboxes) {
			item {
				GuideCheckbox(
					store = liveTvPreferences,
					preference = option.preference,
					heading = option.heading,
					key = option.key,
					caption = option.caption,
				)
			}
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.lbl_show_indicators)) }) }

		items(indicators) { (preference, label) ->
			var enabled by preference

			ListButton(
				headingContent = { Text(label) },
				slots = ListItemSlots(
					trailingContent = { Checkbox(checked = enabled) },
				),
				onClick = { enabled = !enabled },
				modifier = Modifier.focusKey("indicator_$label")
			)
		}
	}
}

/** One of the checkboxes of the guide options: the [preference] it changes, its [heading] and optional [caption]. */
private data class GuideCheckboxOption(
	val preference: Preference<Boolean>,
	@StringRes val heading: Int,
	val key: String,
	@StringRes val caption: Int? = null,
)

private val guideCheckboxes = listOf(
	GuideCheckboxOption(LiveTvPreferences.favsAtTop, R.string.lbl_start_favorites, "favs_at_top"),
	// Velella
	GuideCheckboxOption(
		preference = LiveTvPreferences.restoreLastChannel,
		heading = R.string.velella_pref_guide_restore_last_channel,
		key = "restore_last_channel",
		caption = R.string.velella_pref_guide_restore_last_channel_description,
	),
	GuideCheckboxOption(LiveTvPreferences.colorCodeGuide, R.string.lbl_colored_backgrounds, "color_code_guide"),
)

@Composable
private fun ChannelOrderButton(onClick: () -> Unit) {
	val liveTvPreferences = koinInject<LiveTvPreferences>()
	val channelOrder by rememberPreference(liveTvPreferences, LiveTvPreferences.channelOrder)

	ListButton(
		headingContent = { Text(stringResource(R.string.lbl_sort_by)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(LiveTvChannelOrder.fromString(channelOrder).nameRes)) },
		),
		onClick = onClick,
		modifier = Modifier.focusKey(Routes.LIVETV_GUIDE_CHANNEL_ORDER)
	)
}

/** A checkbox for a boolean live TV preference, with an optional caption. */
@Composable
private fun GuideCheckbox(
	store: LiveTvPreferences,
	preference: Preference<Boolean>,
	@StringRes heading: Int,
	key: String,
	@StringRes caption: Int? = null,
) {
	var checked by rememberPreference(store, preference)

	ListButton(
		headingContent = { Text(stringResource(heading)) },
		slots = ListItemSlots(
			captionContent = if (caption != null) ({ Text(stringResource(caption)) }) else null,
			trailingContent = { Checkbox(checked = checked) },
		),
		onClick = { checked = !checked },
		modifier = Modifier.focusKey(key)
	)
}
