package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun Button(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = ButtonDefaults.Shape,
	colors: ButtonColors = ButtonDefaults.colors(),
	content: @Composable RowScope.() -> Unit
) {
	ButtonBase(
		onClick = onClick,
		modifier = modifier,
		style = ButtonStyle(shape = shape, colors = colors),
	) {
		ButtonRow(content = content)
	}
}

@Composable
fun ProgressButton(
	progress: Float,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	colors: ButtonColors = ButtonDefaults.colors(),
	content: @Composable RowScope.() -> Unit
) {
	ProgressButtonBase(
		progress = progress,
		onClick = onClick,
		modifier = modifier,
		style = ButtonStyle(shape = ButtonDefaults.Shape, colors = colors),
	) {
		ButtonRow(content = content)
	}
}

@Composable
private fun ButtonRow(
	contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
	content: @Composable RowScope.() -> Unit,
) = Row(
	modifier = Modifier
		.defaultMinSize(
			minWidth = 58.dp,
			minHeight = 40.dp
		)
		.padding(contentPadding),
	horizontalArrangement = Arrangement.Center,
	verticalAlignment = Alignment.CenterVertically,
	content = content
)
