package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.store.SharedPreferenceStore

/** Ephyra: which one-time hints the user has dismissed (integration/ShieldDisplayHint.kt). */
class HintPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences("ephyra_hints", Context.MODE_PRIVATE)
) {
	companion object {
		/** The home-screen hint about the Shield's display settings was dismissed. */
		val shieldDisplayHintDismissed = booleanPreference("shield_display_hint_dismissed", false)
	}
}
