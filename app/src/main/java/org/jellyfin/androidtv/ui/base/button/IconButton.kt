package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun IconButton(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	colors: ButtonColors = ButtonDefaults.colors(),
	content: @Composable BoxScope.() -> Unit
) {
	ButtonBase(
		onClick = onClick,
		modifier = modifier,
		style = ButtonStyle(shape = IconButtonDefaults.Shape, colors = colors, enabled = enabled),
	) {
		Box(
			modifier = Modifier
				.padding(IconButtonDefaults.ContentPadding),
			content = content,
		)
	}
}
