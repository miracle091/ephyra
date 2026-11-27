package org.jellyfin.androidtv.preference.constant

import androidx.annotation.ColorRes
import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/**
 * Velella: the accent colour of the app, chosen apart from the [AppTheme]. [THEME] keeps the accent of the theme; the others
 * replace it. A fill in the accent carries [onAccent] text, [accentSoft] is the dark tint behind an active button.
 *
 * The colours are the `velella_accent_*` resources of `theme_velella_accent.xml`, the single source for the Compose palette and for
 * the `ThemeOverlay.Velella.Accent.*` styles that apply the accent to the Views. An "Automatic" choice (accent from the artwork) can
 * be added later as one more entry.
 */
enum class AccentColor(
	override val nameRes: Int,
	/** The colour of the accent, or null to keep the theme's. */
	@ColorRes val accent: Int?,
	@ColorRes val onAccent: Int?,
	@ColorRes val accentSoft: Int?,
	/** The `ThemeOverlay.Velella.Accent.*` style that applies the accent to the Views, or null to keep the theme's. */
	val overlayRes: Int?,
) : PreferenceEnum {
	THEME(R.string.velella_accent_theme, null, null, null, null),
	VIOLET(
		R.string.velella_accent_violet,
		R.color.velella_accent_violet,
		R.color.velella_accent_violet_on,
		R.color.velella_accent_violet_soft,
		R.style.ThemeOverlay_Velella_Accent_Violet,
	),
	BLUE(
		R.string.velella_accent_blue,
		R.color.velella_accent_blue,
		R.color.velella_accent_blue_on,
		R.color.velella_accent_blue_soft,
		R.style.ThemeOverlay_Velella_Accent_Blue,
	),
	TEAL(
		R.string.velella_accent_teal,
		R.color.velella_accent_teal,
		R.color.velella_accent_teal_on,
		R.color.velella_accent_teal_soft,
		R.style.ThemeOverlay_Velella_Accent_Teal,
	),
	GREEN(
		R.string.velella_accent_green,
		R.color.velella_accent_green,
		R.color.velella_accent_green_on,
		R.color.velella_accent_green_soft,
		R.style.ThemeOverlay_Velella_Accent_Green,
	),
	AMBER(
		R.string.velella_accent_amber,
		R.color.velella_accent_amber,
		R.color.velella_accent_amber_on,
		R.color.velella_accent_amber_soft,
		R.style.ThemeOverlay_Velella_Accent_Amber,
	),
	CORAL(
		R.string.velella_accent_coral,
		R.color.velella_accent_coral,
		R.color.velella_accent_coral_on,
		R.color.velella_accent_coral_soft,
		R.style.ThemeOverlay_Velella_Accent_Coral,
	),
	RED(
		R.string.velella_accent_red,
		R.color.velella_accent_red,
		R.color.velella_accent_red_on,
		R.color.velella_accent_red_soft,
		R.style.ThemeOverlay_Velella_Accent_Red,
	),
}
