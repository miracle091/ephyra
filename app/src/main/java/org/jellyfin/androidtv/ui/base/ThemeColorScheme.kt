package org.jellyfin.androidtv.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.design.token.EphyraColorTokens
import org.jellyfin.design.token.EphyraPalette
import org.koin.compose.koinInject

/** Ephyra: the design system tokens of the current theme, for colours [ColorScheme] has no field for. */
val LocalEphyraPalette = staticCompositionLocalOf { EphyraColorTokens.dark }

/** Ephyra: the design system tokens of the user's [AppTheme]. */
fun paletteFor(theme: AppTheme): EphyraPalette = when (theme) {
	AppTheme.DARK -> EphyraColorTokens.dark
	AppTheme.BLACK -> EphyraColorTokens.black
	AppTheme.ABYSS -> EphyraColorTokens.abyss
	AppTheme.CORAL -> EphyraColorTokens.coral
	AppTheme.MEDUSA -> EphyraColorTokens.medusa
}

/**
 * Ephyra: the Compose colours of the Ephyra design system for the user's [AppTheme].
 */
fun colorSchemeFor(theme: AppTheme): ColorScheme = ephyraColorScheme(paletteFor(theme))

/** Ephyra: [JellyfinTheme] with the colours of [theme], which also provides its tokens as [LocalEphyraPalette]. */
@Composable
fun EphyraTheme(theme: AppTheme, content: @Composable () -> Unit) {
	val palette = paletteFor(theme)
	CompositionLocalProvider(LocalEphyraPalette provides palette) {
		JellyfinTheme(colorScheme = ephyraColorScheme(palette), content = content)
	}
}

/** Ephyra: [EphyraTheme] with the user's theme setting, for Compose views outside [org.jellyfin.androidtv.ui.browsing.MainActivity]. */
@Composable
fun EphyraTheme(content: @Composable () -> Unit) {
	val userPreferences = koinInject<UserPreferences>()
	EphyraTheme(userPreferences[UserPreferences.appTheme], content)
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
