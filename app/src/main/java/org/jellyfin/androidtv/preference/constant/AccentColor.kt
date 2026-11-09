package org.jellyfin.androidtv.preference.constant

import androidx.annotation.ColorRes
import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/**
 * Ephyra: the accent colour of the app, chosen apart from the [AppTheme]. [THEME] keeps the accent of the theme; the others
 * replace it. A fill in the accent carries [onAccent] text, [accentSoft] is the dark tint behind an active button.
 *
 * The colours are the `ephyra_accent_*` resources of `theme_ephyra_accent.xml`, the single source for the Compose palette and for
 * the `ThemeOverlay.Ephyra.Accent.*` styles that apply the accent to the Views. An "Automatic" choice (accent from the artwork) can
 * be added later as one more entry.
 */
enum class AccentColor(
	override val nameRes: Int,
	/** The colour of the accent, or null to keep the theme's. */
	@ColorRes val accent: Int?,
	@ColorRes val onAccent: Int?,
	@ColorRes val accentSoft: Int?,
	/** The `ThemeOverlay.Ephyra.Accent.*` style that applies the accent to the Views, or null to keep the theme's. */
	val overlayRes: Int?,
) : PreferenceEnum {
	THEME(R.string.ephyra_accent_theme, null, null, null, null),
	VIOLET(
		R.string.ephyra_accent_violet,
		R.color.ephyra_accent_violet,
		R.color.ephyra_accent_violet_on,
		R.color.ephyra_accent_violet_soft,
		R.style.ThemeOverlay_Ephyra_Accent_Violet,
	),
	BLUE(
		R.string.ephyra_accent_blue,
		R.color.ephyra_accent_blue,
		R.color.ephyra_accent_blue_on,
		R.color.ephyra_accent_blue_soft,
		R.style.ThemeOverlay_Ephyra_Accent_Blue,
	),
	TEAL(
		R.string.ephyra_accent_teal,
		R.color.ephyra_accent_teal,
		R.color.ephyra_accent_teal_on,
		R.color.ephyra_accent_teal_soft,
		R.style.ThemeOverlay_Ephyra_Accent_Teal,
	),
	GREEN(
		R.string.ephyra_accent_green,
		R.color.ephyra_accent_green,
		R.color.ephyra_accent_green_on,
		R.color.ephyra_accent_green_soft,
		R.style.ThemeOverlay_Ephyra_Accent_Green,
	),
	AMBER(
		R.string.ephyra_accent_amber,
		R.color.ephyra_accent_amber,
		R.color.ephyra_accent_amber_on,
		R.color.ephyra_accent_amber_soft,
		R.style.ThemeOverlay_Ephyra_Accent_Amber,
	),
	CORAL(
		R.string.ephyra_accent_coral,
		R.color.ephyra_accent_coral,
		R.color.ephyra_accent_coral_on,
		R.color.ephyra_accent_coral_soft,
		R.style.ThemeOverlay_Ephyra_Accent_Coral,
	),
	RED(
		R.string.ephyra_accent_red,
		R.color.ephyra_accent_red,
		R.color.ephyra_accent_red_on,
		R.color.ephyra_accent_red_soft,
		R.style.ThemeOverlay_Ephyra_Accent_Red,
	),
}
