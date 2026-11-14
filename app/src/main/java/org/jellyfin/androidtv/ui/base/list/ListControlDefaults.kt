package org.jellyfin.androidtv.ui.base.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import org.jellyfin.androidtv.ui.base.JellyfinTheme

object ListControlDefaults {
	@ReadOnlyComposable
	@Composable
	fun colors(
		containerColor: Color = JellyfinTheme.colorScheme.listButton,
		focusedContainerColor: Color = JellyfinTheme.colorScheme.listButtonFocused,
	) = ListControlColors(
		containerColor = containerColor,
		focusedContainerColor = focusedContainerColor,
	)

	@ReadOnlyComposable
	@Composable
	fun style(
		colors: ListControlColors = colors(),
		enabled: Boolean = true,
	) = ListControlStyle(colors = colors, enabled = enabled)
}
