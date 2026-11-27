package org.jellyfin.androidtv.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AccentColor
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.design.token.VelellaColorTokens
import org.jellyfin.design.token.VelellaPalette
import org.koin.compose.koinInject

/** Velella: the design system tokens of the current theme, for colours [ColorScheme] has no field for. */
val LocalVelellaPalette = staticCompositionLocalOf { VelellaColorTokens.dark }

/** Velella: the design system tokens of the user's [AppTheme]. */
fun paletteFor(theme: AppTheme): VelellaPalette = when (theme) {
	AppTheme.DARK -> VelellaColorTokens.dark
	AppTheme.BLACK -> VelellaColorTokens.black
	AppTheme.ABYSS -> VelellaColorTokens.abyss
	AppTheme.CORAL -> VelellaColorTokens.coral
	AppTheme.MEDUSA -> VelellaColorTokens.medusa
}

/**
 * Velella: the Compose colours of the Velella design system for the user's [AppTheme].
 */
fun colorSchemeFor(theme: AppTheme): ColorScheme = velellaColorScheme(paletteFor(theme))

/** Velella: the [palette] with the accent tokens of the user's [accent] choice; [AccentColor.THEME] keeps the theme's own. */
fun VelellaPalette.withAccent(accent: AccentColor, resolve: (colorRes: Int) -> Color): VelellaPalette {
	val color = accent.accent ?: return this
	return copy(
		accent = resolve(color),
		onAccent = accent.onAccent?.let(resolve) ?: onAccent,
		accentSoft = accent.accentSoft?.let(resolve) ?: accentSoft,
	)
}

/**
 * Velella: [JellyfinTheme] with the colours of [theme] and the user's accent colour, which also provides its tokens as
 * [LocalVelellaPalette].
 */
@Composable
fun VelellaTheme(theme: AppTheme, content: @Composable () -> Unit) {
	val velellaPreferences = koinInject<VelellaPreferences>()
	val context = LocalContext.current
	val palette = paletteFor(theme).withAccent(velellaPreferences[VelellaPreferences.accentColor]) { Color(context.getColor(it)) }
	CompositionLocalProvider(LocalVelellaPalette provides palette) {
		JellyfinTheme(colorScheme = velellaColorScheme(palette), content = content)
	}
}

/** Velella: [VelellaTheme] with the user's theme setting, for Compose views outside [org.jellyfin.androidtv.ui.browsing.MainActivity]. */
@Composable
fun VelellaTheme(content: @Composable () -> Unit) {
	val userPreferences = koinInject<UserPreferences>()
	VelellaTheme(userPreferences[UserPreferences.appTheme], content)
}

/**
 * The design system's mapping from [ColorScheme] fields to its tokens. Focus is a solid [VelellaPalette.ink] fill with
 * [VelellaPalette.bg] content.
 */
fun velellaColorScheme(palette: VelellaPalette): ColorScheme = with(palette) {
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
