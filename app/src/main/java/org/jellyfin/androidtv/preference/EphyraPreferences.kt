package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.store.SharedPreferenceStore

/** Ephyra: settings of the fork's own features. */
class EphyraPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences("ephyra_settings", Context.MODE_PRIVATE)
) {
	companion object {
		/** Cards of unwatched episodes show a blurred picture instead of a scene from the episode. */
		val spoilerBlur = booleanPreference("spoiler_blur", false)

		/** Episodes queued after the one started stay in its season. */
		val stopAtSeasonEnd = booleanPreference("stop_at_season_end", false)
	}
}
