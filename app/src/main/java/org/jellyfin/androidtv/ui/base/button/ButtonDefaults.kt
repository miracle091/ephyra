package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme

object ButtonDefaults {
	val Shape: Shape = CircleShape
	val ContentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp)

	@ReadOnlyComposable
	@Composable
	fun colors(
		containerColor: Color = JellyfinTheme.colorScheme.button,
		contentColor: Color = JellyfinTheme.colorScheme.onButton,
	) = ButtonColors(
		containerColor = containerColor,
		contentColor = contentColor,
		focusedContainerColor = JellyfinTheme.colorScheme.buttonFocused,
		focusedContentColor = JellyfinTheme.colorScheme.onButtonFocused,
		disabledContainerColor = JellyfinTheme.colorScheme.buttonDisabled,
		disabledContentColor = JellyfinTheme.colorScheme.onButtonDisabled,
	)

	@ReadOnlyComposable
	@Composable
	fun style(
		shape: Shape = Shape,
		colors: ButtonColors = colors(),
		enabled: Boolean = true,
	) = ButtonStyle(shape = shape, colors = colors, enabled = enabled)
}
