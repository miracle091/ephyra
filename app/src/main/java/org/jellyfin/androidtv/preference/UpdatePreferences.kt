package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.longPreference
import org.jellyfin.preference.store.SharedPreferenceStore
import org.jellyfin.preference.stringPreference

/** Ephyra: settings and state of the update check (update/DailyUpdateCheck.kt). */
class UpdatePreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences("ephyra_update", Context.MODE_PRIVATE)
) {
	companion object {
		/** Check GitHub Releases for a new version at most once a day. */
		val dailyCheckEnabled = booleanPreference("daily_check_enabled", true)

		/** When the last successful check ran, in milliseconds since the epoch. */
		val lastCheck = longPreference("last_check", 0L)

		/** The version whose home-screen notice the user dismissed, so it is not shown again. */
		val dismissedVersion = stringPreference("dismissed_version", "")
	}
}
