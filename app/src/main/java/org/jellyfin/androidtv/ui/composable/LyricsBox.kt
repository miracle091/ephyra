package org.jellyfin.androidtv.ui.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.sdk.model.api.LyricDto
import org.jellyfin.sdk.model.extensions.ticks
import kotlin.time.Duration

private data class LyricsBoxContentMeasurements(
	val size: Size,
	val items: List<Measured>,
)

private fun Collection<TimedLine>.indexAtTimestamp(timestamp: Duration) = indexOfLast { it.timestamp <= timestamp }.takeIf { it != -1 }

/** Where the playback is, for lyrics that are not timed: the [timestamp] in the [duration], and whether it is [paused]. */
@Immutable
data class LyricsPosition(
	val timestamp: Duration = Duration.ZERO,
	val duration: Duration = Duration.ZERO,
	val paused: Boolean = false,
)

@Composable
private fun LyricsLine(
	text: String,
	textStyle: TextStyle,
	active: Boolean = false,
	gap: Dp = 15.dp,
) {
	val lineColor = textStyle.color
	val color by animateColorAsState(
		targetValue = if (active) lineColor else lineColor.copy(alpha = 0.5f),
		animationSpec = tween(),
		label = "LyricsLineColor",
	)

	val scale by animateFloatAsState(
		targetValue = if (active) 1.1f else 1f,
		animationSpec = tween(),
		label = "LyricsLineScale",
	)

	Text(
		text = text,
		style = textStyle.merge(color = color, textAlign = TextAlign.Center),
		modifier = Modifier
			.padding(bottom = gap)
			.scale(scale)
	)
}

@Composable
private fun <T> LyricsBoxContent(
	items: Collection<T>,
	modifier: Modifier = Modifier,
	onMeasured: ((measurements: LyricsBoxContentMeasurements) -> Unit)? = null,
	itemContent: @Composable (item: T, index: Int) -> Unit,
) = Layout(
	modifier = modifier,
	measurePolicy = { measurables, constraints ->
		val placeables = measurables.map { measurable ->
			measurable.measure(constraints.copy(minWidth = constraints.maxWidth))
		}
		if (onMeasured != null) {
			val totalHeight = placeables.sumOf { it.height }.toFloat()
			onMeasured(LyricsBoxContentMeasurements(Size(1f, totalHeight), placeables))
		}

		layout(constraints.maxWidth, constraints.maxHeight) {
			// Start at the offset until the active line and half the height of this container to vertically center the item
			var yOffset = constraints.maxHeight / 2

			for (placeable in placeables) {
				placeable.placeRelative(
					x = 0,
					y = yOffset,
				)

				yOffset += placeable.height
			}
		}
	},

	content = {
		items.forEachIndexed { index, item ->
			itemContent(item, index)
		}
	},
)

@Composable
fun LyricsBox(
	lines: List<TimedLine>,
	currentTimestamp: Duration = Duration.ZERO,
	textStyle: TextStyle = LocalTextStyle.current,
) {
	var lineMeasurements by remember { mutableStateOf<List<Measured>>(emptyList()) }
	val activeLine = lines.indexAtTimestamp(currentTimestamp)
	val activeLineOffsetAnimation = remember { Animatable(0f) }

	LaunchedEffect(activeLine) {
		var offset = 0f

		if (activeLine != null) {
			// Add offset for all previous items
			offset += lineMeasurements.take(activeLine).sumOf { it.measuredHeight }
			// Add offset for half-height to center item
			offset += lineMeasurements.getOrNull(activeLine)?.measuredHeight?.div(2) ?: 0
		}

		activeLineOffsetAnimation.animateTo(offset)
	}

	LyricsBoxContent(
		items = lines,
		modifier = Modifier.graphicsLayer {
			translationY = -activeLineOffsetAnimation.value
		},
		onMeasured = { measurements -> lineMeasurements = measurements.items }
	) { line, index ->
		LyricsLine(
			text = line.text,
			active = index == activeLine,
			textStyle = textStyle,
		)
	}
}

@Composable
fun LyricsBox(
	lines: List<String>,
	modifier: Modifier = Modifier,
	position: LyricsPosition = LyricsPosition(),
	textStyle: TextStyle = LocalTextStyle.current,
) = Box(modifier) {
	var totalHeight by remember { mutableFloatStateOf(0f) }
	val progress by rememberPlayerProgress(!position.paused, position.timestamp, position.duration)

	LyricsBoxContent(
		items = lines,
		modifier = Modifier.graphicsLayer {
			translationY = -(progress * totalHeight)
		},
		onMeasured = { measurements -> totalHeight = measurements.size.height }
	) { line, index ->
		LyricsLine(
			text = line,
			textStyle = textStyle,
		)
	}
}

@Composable
fun LyricsDtoBox(
	lyricDto: LyricDto,
	modifier: Modifier = Modifier,
	position: LyricsPosition = LyricsPosition(),
	textStyle: TextStyle = LocalTextStyle.current,
) = Box(modifier) {
	val lyrics = lyricDto.lyrics
	val isTimed = lyrics.firstOrNull()?.start != null
	if (isTimed) {
		LyricsBox(
			lines = lyrics.map {
				TimedLine(it.start?.ticks ?: Duration.ZERO, it.text)
			},
			currentTimestamp = position.timestamp,
			textStyle = textStyle,
		)
	} else {
		LyricsBox(
			lines = lyrics.map { it.text },
			position = position,
			textStyle = textStyle,
		)
	}
}
