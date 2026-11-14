package org.jellyfin.androidtv.ui.base.form

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import org.jellyfin.androidtv.ui.base.JellyfinTheme

@Immutable
data class RangeControlColors(
	val backgroundColor: Color,
	val fillColor: Color,
	val knobColor: Color,
)

object RangeControlDefaults {
	@ReadOnlyComposable
	@Composable
	fun colors(
		backgroundColor: Color = JellyfinTheme.colorScheme.rangeControlBackground,
		fillColor: Color = JellyfinTheme.colorScheme.rangeControlFill,
		knobColor: Color = JellyfinTheme.colorScheme.rangeControlKnob,
	) = RangeControlColors(
		backgroundColor = backgroundColor,
		fillColor = fillColor,
		knobColor = knobColor,
	)
}

private const val FOCUSED_KNOB_SCALE = 1.75f
private const val MARKER_WIDTH = 3f

/** What a [RangeControl] shows: the current [value] between [min] and [max], moved by [step] with [markers] drawn on the bar. */
@Immutable
data class RangeControlState(
	val value: Float = 0f,
	val min: Float = 0f,
	val max: Float = 1f,
	val step: Float = 0.01f,
	val markers: List<Float> = emptyList(),
)

@Composable
fun RangeControl(
	state: RangeControlState,
	modifier: Modifier = Modifier,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	onValueChange: ((value: Float) -> Unit)? = null,
	colors: RangeControlColors = RangeControlDefaults.colors(),
) {
	val value = state.value
	val min = state.min
	val max = state.max
	val markers = state.markers
	val focused by interactionSource.collectIsFocusedAsState()
	val knobSize by animateFloatAsState(if (focused) FOCUSED_KNOB_SCALE else 1f)

	Box(
		modifier = modifier
			.rangeKeyEvents(state, onValueChange)
			.focusable(interactionSource = interactionSource)
			.drawWithContent {
				val barCornerRadius = CornerRadius(size.minDimension, size.minDimension)

				// Background bar
				drawRoundRect(
					color = colors.backgroundColor,
					cornerRadius = barCornerRadius,
				)

				// Get percental value based on min/max options
				val valuePercentage = (value - min) / (max - min)

				// Value fill bar
				if (valuePercentage > 0) {
					drawRoundRect(
						color = colors.fillColor,
						size = size.copy(
							width = valuePercentage * size.width,
						),
						cornerRadius = barCornerRadius,
					)
				}

				// Markers
				for (marker in markers) {
					val markerPercentage = (marker - min) / (max - min)
					if (markerPercentage in 0f..1f) {
						drawRect(
							color = colors.knobColor,
							topLeft = Offset(x = markerPercentage * size.width - MARKER_WIDTH / 2, y = 0f),
							size = size.copy(width = MARKER_WIDTH),
						)
					}
				}

				// Progress knob
				drawCircle(
					color = colors.knobColor,
					center = center.copy(
						x = valuePercentage * size.width,
					),
					radius = size.minDimension * knobSize,
				)
			}
	)
}

/** Moves the value of [state] by its step with the left and right keys, consuming them. */
private fun Modifier.rangeKeyEvents(state: RangeControlState, onValueChange: ((value: Float) -> Unit)?) = onKeyEvent {
	val isForward = it.key == Key.DirectionRight
	val isRewind = it.key == Key.DirectionLeft
	val isScrubbing = isForward || isRewind
	val isKeyDown = it.type == KeyEventType.KeyDown

	val newValue = when {
		isKeyDown && isForward -> (state.value + state.step).coerceAtMost(state.max)
		isKeyDown && isRewind -> (state.value - state.step).coerceAtLeast(state.min)
		else -> state.value
	}

	if (state.value != newValue && onValueChange != null) onValueChange(newValue)

	isScrubbing
}
