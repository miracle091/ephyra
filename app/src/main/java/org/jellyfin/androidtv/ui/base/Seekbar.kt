package org.jellyfin.androidtv.ui.base

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.CoroutineScope
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

/** Velella: how long after the last arrow press a seek starts, so scrubbing restarts a transcode once (also the video quick seek). */
val SEEK_COMMIT_DELAY = 600.milliseconds

// Velella: how long the picked position stays drawn after its seek, until the player reports it
private val SEEK_SETTLE_DELAY = 300.milliseconds

private val CommitKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/** Where a [Seekbar] is: the [progress], the buffered part and the [duration], with the distance one key press seeks. */
@Immutable
data class SeekbarPosition(
	val progress: Duration = Duration.ZERO,
	val buffer: Duration = Duration.ZERO,
	val duration: Duration = Duration.ZERO,
	val seekForwardAmount: Duration = duration / SEEK_STEP_DIVISOR,
	val seekRewindAmount: Duration = duration / SEEK_STEP_DIVISOR,
)

/**
 * What a [Seekbar] reports: when scrubbing starts or stops, each position picked with the arrows ([onScrub]) and the position a
 * seek moves to ([onSeek]: once, [SEEK_COMMIT_DELAY] after the last press, or at once with centre).
 */
@Immutable
data class SeekbarCallbacks(
	val onScrubbing: ((scrubbing: Boolean) -> Unit)? = null,
	val onSeek: ((progress: Duration) -> Unit)? = null,
	val onScrub: ((progress: Duration) -> Unit)? = null,
)

/**
 * Velella: how a [Seekbar] looks: its [colors], and whether it keeps a smaller knob without the focus ([knobAlwaysVisible], like
 * the legacy Leanback seek bar) or shows the knob only with the focus.
 */
@Immutable
data class SeekbarStyle(
	val colors: SeekbarColors,
	val knobAlwaysVisible: Boolean = false,
)

@Composable
fun Seekbar(
	position: SeekbarPosition,
	modifier: Modifier = Modifier,
	callbacks: SeekbarCallbacks = SeekbarCallbacks(),
	enabled: Boolean = true,
	style: SeekbarStyle = SeekbarStyle(SeekbarDefaults.colors()),
) {
	val durationMs = position.duration.inWholeMilliseconds.toFloat().coerceAtLeast(1f)
	val onSeek = callbacks.onSeek
	val onScrub = callbacks.onScrub

	SeekbarBar(
		state = SeekbarBarState(
			progress = position.progress.inWholeMilliseconds.toFloat() / durationMs,
			buffer = position.buffer.inWholeMilliseconds.toFloat() / durationMs,
			seekForwardAmount = position.seekForwardAmount.inWholeMilliseconds.toFloat() / durationMs,
			seekRewindAmount = position.seekRewindAmount.inWholeMilliseconds.toFloat() / durationMs,
			enabled = enabled,
		),
		modifier = modifier,
		callbacks = SeekbarBarCallbacks(
			onScrubbing = callbacks.onScrubbing,
			onSeek = if (onSeek == null) null else { progress -> onSeek(progress.toDouble() * position.duration) },
			onScrub = if (onScrub == null) null else { progress -> onScrub(progress.toDouble() * position.duration) },
		),
		style = style,
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

/** The [SeekbarCallbacks] with positions as fractions of the duration. */
private data class SeekbarBarCallbacks(
	val onScrubbing: ((scrubbing: Boolean) -> Unit)?,
	val onSeek: ((progress: Float) -> Unit)?,
	val onScrub: ((progress: Float) -> Unit)?,
)

@Composable
private fun SeekbarBar(
	state: SeekbarBarState,
	modifier: Modifier,
	callbacks: SeekbarBarCallbacks,
	style: SeekbarStyle,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val coroutineScope = rememberCoroutineScope()
	val focused by interactionSource.collectIsFocusedAsState()
	val currentCallbacks by rememberUpdatedState(callbacks)
	val scrub = remember { SeekbarScrub(coroutineScope) { currentCallbacks } }
	val visibleProgress = scrub.progressOverride ?: state.progress
	val knobAlpha by animateFloatAsState(if (focused || style.knobAlwaysVisible) 1f else 0f)
	// Velella: the knob's radius in bar heights: half the focused size without the focus (twice the Leanback knob, readable from
	// across the room), large with it
	val knobScale by animateFloatAsState(if (focused || !style.knobAlwaysVisible) KNOB_SCALE_FOCUSED else KNOB_SCALE_UNFOCUSED)
	DisposableEffect(scrub) { onDispose { scrub.dispose() } }

	Box(
		modifier = modifier
			.onKeyEvent { state.enabled && scrub.onKey(it, state) }
			.focusable(interactionSource = interactionSource, enabled = state.enabled)
			.drawWithContent {
				drawSeekbar(style.colors, state.buffer, visibleProgress, knobAlpha, knobScale)
			}
	)
}

private const val KNOB_SCALE_FOCUSED = 2f
private const val KNOB_SCALE_UNFOCUSED = 1f

/**
 * Velella: scrubbing a [Seekbar] with the arrows. Each press moves the drawn position ([progressOverride], reported with onScrub);
 * one seek follows [SEEK_COMMIT_DELAY] after the last press, or at once with centre, then the picked position stays drawn a
 * moment until the player reports it. If the bar leaves the screen first, the seek still happens and scrubbing ends.
 */
private class SeekbarScrub(
	private val coroutineScope: CoroutineScope,
	private val callbacks: () -> SeekbarBarCallbacks,
) {
	var progressOverride by mutableStateOf<Float?>(null)
		private set
	private var pendingSeek: Float? = null
	private var scrubbing = false
	private var job: Job? = null

	fun onKey(event: KeyEvent, state: SeekbarBarState): Boolean {
		val isKeyDown = event.type == KeyEventType.KeyDown
		if (isKeyDown && event.key in CommitKeys && pendingSeek != null) {
			commit()
			return true
		}

		val isForward = event.key == Key.DirectionRight
		val isRewind = event.key == Key.DirectionLeft
		if (!isForward && !isRewind) return false

		if (isKeyDown) {
			job?.cancel()
			scrubbing = true
			callbacks().onScrubbing?.invoke(true)

			// Read the picked position now, not the one of the last composition, so quick presses all count
			val current = progressOverride ?: state.progress
			val next = if (isForward) (current + state.seekForwardAmount).coerceAtMost(1f)
			else (current - state.seekRewindAmount).coerceAtLeast(0f)
			if (next != current) {
				progressOverride = next
				pendingSeek = next
				callbacks().onScrub?.invoke(next)
			}
		} else if (event.type == KeyEventType.KeyUp) {
			job?.cancel()
			job = coroutineScope.launch {
				delay(SEEK_COMMIT_DELAY)
				commit()
			}
		}
		return true
	}

	private fun commit() {
		job?.cancel()
		pendingSeek?.let { progress -> callbacks().onSeek?.invoke(progress) }
		pendingSeek = null
		job = coroutineScope.launch {
			delay(SEEK_SETTLE_DELAY)
			endScrubbing()
			progressOverride = null
		}
	}

	private fun endScrubbing() {
		if (!scrubbing) return
		scrubbing = false
		callbacks().onScrubbing?.invoke(false)
	}

	fun dispose() {
		job?.cancel()
		pendingSeek?.let { progress -> callbacks().onSeek?.invoke(progress) }
		pendingSeek = null
		endScrubbing()
	}
}

private fun DrawScope.drawSeekbar(
	colors: SeekbarColors,
	buffer: Float,
	visibleProgress: Float,
	knobAlpha: Float,
	knobScale: Float,
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
		radius = size.minDimension * knobScale,
	)
}
