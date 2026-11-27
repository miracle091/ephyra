package org.jellyfin.androidtv.ui.player.video

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.SEEK_COMMIT_DELAY
import org.jellyfin.androidtv.ui.player.base.toast.MediaToastRegistry
import org.jellyfin.playback.core.PlaybackManager
import kotlin.time.Duration
import java.util.Locale

private val CENTRE_KEYS = setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER)

/** Velella: the quick seek's next target from [current]: one fast forward or rewind step, kept between the start and the end. */
fun quickSeekTarget(
	current: Duration,
	forward: Boolean,
	forwardAmount: Duration,
	rewindAmount: Duration,
	duration: Duration,
): Duration {
	val next = if (forward) current + forwardAmount else current - rewindAmount
	return next.coerceIn(Duration.ZERO, duration.coerceAtLeast(Duration.ZERO))
}

/** Velella: the quick seek's distance from where it started, signed, as m:ss or h:mm:ss ("+1:30", "−0:20"). */
fun quickSeekLabel(offset: Duration): String {
	val sign = if (offset.isNegative()) "−" else "+"
	return offset.absoluteValue.toComponents { hours, minutes, seconds, _ ->
		if (hours > 0) "%s%d:%02d:%02d".format(Locale.ROOT, sign, hours, minutes, seconds)
		else "%s%d:%02d".format(Locale.ROOT, sign, minutes, seconds)
	}
}

/**
 * Velella: left and right while the controls are hidden. Each press moves a target by the user's fast forward or rewind amount and
 * shows it in a toast (the distance, and the target on the ring); one seek follows [SEEK_COMMIT_DELAY] after the last press, so a
 * transcode restarts once. Centre during that wait seeks at once.
 */
class QuickSeek internal constructor(
	private val playbackManager: PlaybackManager,
	private val mediaToastRegistry: MediaToastRegistry,
	private val coroutineScope: CoroutineScope,
) {
	private var start: Duration? = null
	private var target: Duration = Duration.ZERO
	private var commitJob: Job? = null

	/** The quick seek keys: true when [action] was one (centre while a seek waits seeks at once instead of pausing). */
	fun onKey(action: VideoPlayerKeyAction, keyCode: Int): Boolean = when {
		action == VideoPlayerKeyAction.QUICK_SEEK_FORWARD -> press(forward = true)
		action == VideoPlayerKeyAction.QUICK_SEEK_REWIND -> press(forward = false)
		start != null && keyCode in CENTRE_KEYS -> commit()
		else -> false
	}

	private fun press(forward: Boolean): Boolean {
		val positionInfo = playbackManager.state.positionInfo
		val duration = positionInfo.duration
		// Nothing to seek in yet (or a live stream): the key is still the quick seek's
		if (duration <= Duration.ZERO) return true

		val from = start ?: positionInfo.active.also {
			start = it
			target = it
		}
		target = quickSeekTarget(
			current = target,
			forward = forward,
			forwardAmount = playbackManager.options.defaultFastForwardAmount(),
			rewindAmount = playbackManager.options.defaultRewindAmount(),
			duration = duration,
		)
		mediaToastRegistry.emit(
			icon = if (forward) R.drawable.ic_fast_forward else R.drawable.ic_rewind,
			progress = (target / duration).toFloat(),
			label = quickSeekLabel(target - from),
		)

		commitJob?.cancel()
		commitJob = coroutineScope.launch {
			delay(SEEK_COMMIT_DELAY)
			commit()
		}
		return true
	}

	/** Seeks to the picked target if a quick seek waits; true then. */
	fun commit(): Boolean {
		commitJob?.cancel()
		commitJob = null
		if (start == null) return false
		start = null
		playbackManager.state.seek(target)
		return true
	}
}

@Composable
fun rememberQuickSeek(playbackManager: PlaybackManager, mediaToastRegistry: MediaToastRegistry): QuickSeek {
	val coroutineScope = rememberCoroutineScope()
	val quickSeek = remember(playbackManager, mediaToastRegistry) { QuickSeek(playbackManager, mediaToastRegistry, coroutineScope) }
	// A press just before the player closes still seeks, so the reported position is the one picked
	DisposableEffect(quickSeek) { onDispose { quickSeek.commit() } }
	return quickSeek
}
