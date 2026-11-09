package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.androidtv.preference.constant.AccentColor
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.intPreference
import org.jellyfin.preference.store.SharedPreferenceStore

/** Ephyra: settings of the fork's own features. */
class EphyraPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences("ephyra_settings", Context.MODE_PRIVATE)
) {
	companion object {
		/** The spotlight at the top of the home screen: recent unwatched films and series, in turn. */
		val homeSpotlight = booleanPreference("home_spotlight", true)

		/** The Playlists library among the home's "My media" cards. */
		val homePlaylists = booleanPreference("home_playlists", false)

		/** Cards of unwatched episodes show a blurred picture instead of a scene from the episode. */
		val spoilerBlur = booleanPreference("spoiler_blur", false)

		/** Episodes queued after the one started stay in its season. */
		val stopAtSeasonEnd = booleanPreference("stop_at_season_end", false)

		/** Seconds the skip button stays on screen before it hides by itself; 0 keeps it until the segment ends. */
		val skipAutoHideSeconds = intPreference("skip_auto_hide_seconds", 15)

		/** The accent colour, apart from the theme; [AccentColor.THEME] keeps the theme's own. */
		val accentColor = enumPreference("accent_color", AccentColor.THEME)

		/** The episodes of a season as rows with the picture on the left and the plot beside it, instead of a row of cards. */
		val compactEpisodeList = booleanPreference("compact_episode_list", false)
	}
}
