package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class AppTheme(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * The default dark theme
	 */
	DARK(R.string.pref_theme_dark),

	// Velella: no Emerald and Muted purple themes; a saved value of either reads as the default, DARK

	/**
	 * Velella: true black backgrounds, for OLED TVs
	 */
	BLACK(R.string.velella_pref_theme_black),

	/**
	 * Velella: deep teal with a mint accent
	 */
	ABYSS(R.string.velella_pref_theme_abyss),

	/**
	 * Velella: warm dark red with a coral accent
	 */
	CORAL(R.string.velella_pref_theme_coral),

	/**
	 * Velella: deep violet with a lilac accent
	 */
	MEDUSA(R.string.velella_pref_theme_medusa),
}
