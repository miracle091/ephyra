package org.jellyfin.androidtv.ui.base

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.times

@Immutable
data class SeekbarColors(
	val backgroundColor: Color,
	val bufferColor: Color,
	val progressColor: Color,
	val knobColor: Color,
)

object SeekbarDefaults {
	@ReadOnlyComposable
	@Composable
	fun colors(
		backgroundColor: Color = JellyfinTheme.colorScheme.rangeControlBackground,
		bufferColor: Color = JellyfinTheme.colorScheme.seekbarBuffer,
		progressColor: Color = JellyfinTheme.colorScheme.rangeControlFill,
		knobColor: Color = JellyfinTheme.colorScheme.rangeControlKnob,
	) = SeekbarColors(
		backgroundColor = backgroundColor,
		bufferColor = bufferColor,
		progressColor = progressColor,
		knobColor = knobColor,
	)
}

private const val SEEK_STEP_DIVISOR = 100

/** Where a [Seekbar] is: the [progress], the buffered part and the [duration], with the distance one key press seeks. */
@Immutable
data class SeekbarPosition(
	val progress: Duration = Duration.ZERO,
	val buffer: Duration = Duration.ZERO,
	val duration: Duration = Duration.ZERO,
	val seekForwardAmount: Duration = duration / SEEK_STEP_DIVISOR,
	val seekRewindAmount: Duration = duration / SEEK_STEP_DIVISOR,
)

/** What a [Seekbar] reports: when scrubbing starts or stops and the position a seek moves to. */
@Immutable
data class SeekbarCallbacks(
	val onScrubbing: ((scrubbing: Boolean) -> Unit)? = null,
	val onSeek: ((progress: Duration) -> Unit)? = null,
)

@Composable
fun Seekbar(
	position: SeekbarPosition,
	modifier: Modifier = Modifier,
	callbacks: SeekbarCallbacks = SeekbarCallbacks(),
	enabled: Boolean = true,
	colors: SeekbarColors = SeekbarDefaults.colors(),
) {
	val durationMs = position.duration.inWholeMilliseconds.toFloat().coerceAtLeast(1f)
	val onSeek = callbacks.onSeek

	SeekbarBar(
		state = SeekbarBarState(
			progress = position.progress.inWholeMilliseconds.toFloat() / durationMs,
			buffer = position.buffer.inWholeMilliseconds.toFloat() / durationMs,
			seekForwardAmount = position.seekForwardAmount.inWholeMilliseconds.toFloat() / durationMs,
			seekRewindAmount = position.seekRewindAmount.inWholeMilliseconds.toFloat() / durationMs,
			enabled = enabled,
		),
		modifier = modifier,
		onScrubbing = callbacks.onScrubbing,
		onSeek = if (onSeek == null) null else { progress -> onSeek(progress.toDouble() * position.duration) },
		colors = colors,
	)
}

/** The [Seekbar] position as fractions of its duration, and whether it reacts to keys. */
private data class SeekbarBarState(
	val progress: Float,
	val buffer: Float,
	val seekForwardAmount: Float,
	val seekRewindAmount: Float,
	val enabled: Boolean,
)

@Composable
private fun SeekbarBar(
	state: SeekbarBarState,
	modifier: Modifier,
	onScrubbing: ((scrubbing: Boolean) -> Unit)?,
	onSeek: ((progress: Float) -> Unit)?,
	colors: SeekbarColors,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val coroutineScope = rememberCoroutineScope()
	val focused by interactionSource.collectIsFocusedAsState()
	var progressOverride by remember { mutableStateOf<Float?>(null) }
	val visibleProgress = progressOverride ?: state.progress
	val knobAlpha by animateFloatAsState(if (focused) 1f else 0f)
	var scrubCancelJob by remember { mutableStateOf<Job?>(null) }

	Box(
		modifier = modifier
			.onKeyEvent {
				if (!state.enabled) return@onKeyEvent false

				val isForward = it.key == Key.DirectionRight
				val isRewind = it.key == Key.DirectionLeft
				val isScrubbing = isForward || isRewind
				val isKeyUp = it.type == KeyEventType.KeyUp
				val isKeyDown = it.type == KeyEventType.KeyDown

				val newProgress = nextProgress(
					current = visibleProgress,
					state = state,
					isKeyDown = isKeyDown,
					isForward = isForward,
					isRewind = isRewind,
				)

				if (isScrubbing && isKeyDown && onScrubbing != null) {
					scrubCancelJob?.cancel()
					onScrubbing(true)
				}

				if (visibleProgress != newProgress) {
					progressOverride = newProgress
					if (onSeek != null) onSeek(newProgress)
				}

				if (isScrubbing && isKeyUp && onScrubbing != null) {
					scrubCancelJob?.cancel()
					scrubCancelJob = coroutineScope.launch {
						delay(300.milliseconds)
						onScrubbing(false)
						progressOverride = null
					}
				}

				return@onKeyEvent isScrubbing
			}
			.focusable(interactionSource = interactionSource, enabled = state.enabled)
			.drawWithContent {
				drawSeekbar(colors, state.buffer, visibleProgress, knobAlpha)
			}
	)
}

private fun nextProgress(
	current: Float,
	state: SeekbarBarState,
	isKeyDown: Boolean,
	isForward: Boolean,
	isRewind: Boolean,
) = when {
	isKeyDown && isForward -> (current + state.seekForwardAmount).coerceAtMost(1f)
	isKeyDown && isRewind -> (current - state.seekRewindAmount).coerceAtLeast(0f)
	else -> current
}

private fun DrawScope.drawSeekbar(
	colors: SeekbarColors,
	buffer: Float,
	visibleProgress: Float,
	knobAlpha: Float,
) {
	val barCornerRadius = CornerRadius(size.minDimension, size.minDimension)

	// Background bar
	drawRoundRect(
		color = colors.backgroundColor,
		cornerRadius = barCornerRadius,
	)

	// Buffer bar
	if (buffer > 0f) {
		drawRoundRect(
			color = colors.bufferColor,
			size = size.copy(
				width = buffer * size.width,
			),
			cornerRadius = barCornerRadius,
		)
	}

	// Progress bar
	if (visibleProgress > 0f) {
		drawRoundRect(
			color = colors.progressColor,
			size = size.copy(
				width = visibleProgress * size.width,
			),
			cornerRadius = barCornerRadius,
		)
	}

	// Progress knob
	drawCircle(
		color = colors.knobColor,
		alpha = knobAlpha,
		center = center.copy(
			x = visibleProgress * size.width,
		),
		radius = size.minDimension * 2,
	)
}
