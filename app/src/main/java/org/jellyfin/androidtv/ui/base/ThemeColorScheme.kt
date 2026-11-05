package org.jellyfin.androidtv.ui.base

import androidx.compose.ui.graphics.Color
import org.jellyfin.androidtv.preference.constant.AppTheme

/**
 * Ephyra: the Compose colours for the user's [AppTheme]. Only the black theme differs from upstream's single scheme, so the
 * settings panel turns black with the Leanback screens (same greys as res/values/theme_ephyra_black.xml).
 */
fun colorSchemeFor(theme: AppTheme): ColorScheme = when (theme) {
	AppTheme.BLACK -> colorScheme().copy(
		background = Color.Black,
		surface = Color(0xFF141414),
		listButtonFocused = Color(0xFF2E2E2E),
	)

	AppTheme.DARK, AppTheme.EMERALD, AppTheme.MUTED_PURPLE -> colorScheme()
}
