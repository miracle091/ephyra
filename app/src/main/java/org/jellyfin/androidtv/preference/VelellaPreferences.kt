package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.androidtv.preference.constant.AccentColor
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.intPreference
import org.jellyfin.preference.stringPreference
import org.jellyfin.preference.store.SharedPreferenceStore

/** Velella: settings of the fork's own features. */
class VelellaPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences("velella_settings", Context.MODE_PRIVATE)
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

		/** The user's own Invidious instance for trailers, tried before the public ones; empty for none. */
		val invidiousInstance = stringPreference("invidious_instance", "")

		/** The Invidious instance that last gave a trailer's streams, tried next time before the public list. */
		val invidiousLastInstance = stringPreference("invidious_last_instance", "")
	}
}
