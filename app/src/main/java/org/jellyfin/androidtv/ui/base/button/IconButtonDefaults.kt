package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme

object IconButtonDefaults {
	val Shape: Shape = ButtonDefaults.Shape
	val ContentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 10.dp)

	@ReadOnlyComposable
	@Composable
	fun colors(
		containerColor: Color = JellyfinTheme.colorScheme.button,
		contentColor: Color = JellyfinTheme.colorScheme.onButton,
		focusedContainerColor: Color = JellyfinTheme.colorScheme.buttonFocused,
		focusedContentColor: Color = JellyfinTheme.colorScheme.onButtonFocused,
		disabledContainerColor: Color = JellyfinTheme.colorScheme.buttonDisabled,
		disabledContentColor: Color = JellyfinTheme.colorScheme.onButtonDisabled,
	) = ButtonDefaults.colors(
		containerColor = containerColor,
		contentColor = contentColor,
		focusedContainerColor = focusedContainerColor,
		focusedContentColor = focusedContentColor,
		disabledContainerColor = disabledContainerColor,
		disabledContentColor = disabledContentColor,
	)
}
