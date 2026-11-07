package org.jellyfin.androidtv.ui.base

import androidx.compose.ui.graphics.Color
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.design.token.EphyraColorTokens
import org.jellyfin.design.token.EphyraPalette

/**
 * Ephyra: the Compose colours for the user's [AppTheme]. Dark and Black use the Ephyra design system; the other upstream themes
 * keep upstream's single scheme.
 */
fun colorSchemeFor(theme: AppTheme): ColorScheme = when (theme) {
	AppTheme.DARK -> ephyraColorScheme(EphyraColorTokens.dark)
	AppTheme.BLACK -> ephyraColorScheme(EphyraColorTokens.black)
	AppTheme.EMERALD, AppTheme.MUTED_PURPLE -> colorScheme()
}

/**
 * The design system's mapping from [ColorScheme] fields to its tokens. Focus is a solid [EphyraPalette.ink] fill with
 * [EphyraPalette.bg] content.
 */
fun ephyraColorScheme(palette: EphyraPalette): ColorScheme = with(palette) {
	ColorScheme(
		background = bg,
		onBackground = ink,
		button = surface2,
		onButton = ink,
		buttonFocused = ink,
		onButtonFocused = bg,
		buttonDisabled = surface1,
		onButtonDisabled = inkFaint,
		buttonActive = accentSoft,
		onButtonActive = ink,
		input = surface2,
		onInput = ink,
		inputFocused = ink,
		onInputFocused = bg,
		rangeControlBackground = surface3,
		rangeControlFill = accent,
		rangeControlKnob = ink,
		seekbarBuffer = inkFaint,
		recording = danger,
		onRecording = onDanger,
		badge = accent,
		onBadge = onAccent,
		listHeader = ink,
		listOverline = inkFaint,
		listHeadline = ink,
		listCaption = inkMuted,
		listButton = Color.Transparent,
		listButtonFocused = surface3,
		surface = surface2,
		scrim = scrim,
	)
}
