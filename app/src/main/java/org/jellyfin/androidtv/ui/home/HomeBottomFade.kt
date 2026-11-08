package org.jellyfin.androidtv.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette

private val HomeBottomFadeHeight = 48.dp

/** Ephyra: the bottom edge of the home rows fades into the theme's background, so a row cut by the screen edge fades out. */
@Composable
fun Modifier.homeBottomFade(): Modifier {
	val bg = LocalEphyraPalette.current.bg
	return drawWithContent {
		drawContent()
		val height = HomeBottomFadeHeight.toPx().coerceAtMost(size.height)
		val top = size.height - height
		drawRect(
			brush = Brush.verticalGradient(0f to bg.copy(alpha = 0f), 1f to bg, startY = top, endY = size.height),
			topLeft = Offset(0f, top),
			size = Size(size.width, height),
		)
	}
}
