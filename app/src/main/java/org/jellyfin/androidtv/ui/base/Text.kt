package org.jellyfin.androidtv.ui.base

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * Text with the typography in [style]: use `LocalTextStyle.current.merge(...)` to change only some parts of the current style.
 * When [style] has no color, black is used.
 */
@Composable
fun Text(
	text: String,
	modifier: Modifier = Modifier,
	overflow: TextOverflow = TextOverflow.Clip,
	maxLines: Int = Int.MAX_VALUE,
	style: TextStyle = LocalTextStyle.current
) {
	BasicText(
		text = text,
		modifier = modifier.graphicsLayer { this.compositingStrategy = CompositingStrategy.Offscreen },
		style = style.merge(color = style.color.takeOrElse { Color.Black }),
		overflow = overflow,
		maxLines = maxLines,
	)
}

@Composable
fun Text(
	text: AnnotatedString,
	modifier: Modifier = Modifier,
	overflow: TextOverflow = TextOverflow.Clip,
	maxLines: Int = Int.MAX_VALUE,
	style: TextStyle = LocalTextStyle.current
) {
	BasicText(
		text = text,
		modifier = modifier.graphicsLayer { this.compositingStrategy = CompositingStrategy.Offscreen },
		style = style.merge(color = style.color.takeOrElse { Color.Black }),
		overflow = overflow,
		maxLines = maxLines,
	)
}

val LocalTextStyle = compositionLocalOf(structuralEqualityPolicy()) { TypographyDefaults.Default }

@Composable
fun ProvideTextStyle(value: TextStyle, content: @Composable () -> Unit) {
	val mergedStyle = LocalTextStyle.current.merge(value)
	CompositionLocalProvider(LocalTextStyle provides mergedStyle, content = content)
}
