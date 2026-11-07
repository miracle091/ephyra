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

	// Ephyra: no Emerald and Muted purple themes; a saved value of either reads as the default, DARK

	/**
	 * Ephyra: true black backgrounds, for OLED TVs
	 */
	BLACK(R.string.ephyra_pref_theme_black),

	/**
	 * Ephyra: deep teal with a mint accent
	 */
	ABYSS(R.string.ephyra_pref_theme_abyss),

	/**
	 * Ephyra: warm dark red with a coral accent
	 */
	CORAL(R.string.ephyra_pref_theme_coral),

	/**
	 * Ephyra: deep violet with a lilac accent
	 */
	MEDUSA(R.string.ephyra_pref_theme_medusa),
}
