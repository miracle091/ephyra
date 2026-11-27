package org.jellyfin.androidtv.ui.player.video

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.ui.composable.rememberPlayerPositionInfo
import org.jellyfin.androidtv.ui.playback.overlay.SkipButtonState
import org.jellyfin.androidtv.ui.playback.overlay.isSkipRecallKey
import org.jellyfin.androidtv.ui.playback.overlay.skipAutoHideDelay
import org.jellyfin.androidtv.ui.playback.overlay.skipButtonState
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.koin.compose.koinInject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/** Velella: what a key does to the skip button. */
enum class SkipKeyAction { SKIP, DISMISS, RECALL }

/**
 * Velella: the legacy player's skip button keys (CustomPlaybackOverlayFragment): on a shown button centre skips and back closes it
 * for this segment; a button that hid by itself comes back on centre, up or down. Null when the key is not for the button.
 */
fun skipKeyAction(state: SkipButtonState, keyCode: Int): SkipKeyAction? = when (state) {
	SkipButtonState.SHOWN -> when (keyCode) {
		KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> SkipKeyAction.SKIP
		KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_ESCAPE -> SkipKeyAction.DISMISS
		else -> null
	}
	SkipButtonState.RECALLABLE -> if (isSkipRecallKey(keyCode)) SkipKeyAction.RECALL else null
	SkipButtonState.NONE -> null
}

/** Velella: the skip button of the segment playing now: its [state] and what its keys do ([onKey]). */
class SkipSegment(val state: SkipButtonState, val onKey: (SkipKeyAction) -> Unit)

/**
 * Velella: the skip button for the "ask to skip" segments of [item] (Settings, media segments), as in the legacy player: shown
 * inside such a segment while the controls are hidden ([controlsVisible] false), hidden after the user's delay but called back by
 * a key until the segment ends; skipping jumps to the segment's end.
 */
@Composable
fun rememberSkipSegment(playbackManager: PlaybackManager, item: BaseItemDto?, controlsVisible: Boolean): SkipSegment {
	val repository = koinInject<MediaSegmentRepository>()
	val preferences = koinInject<VelellaPreferences>()

	var segments by remember(item?.id) { mutableStateOf(emptyList<MediaSegmentDto>()) }
	LaunchedEffect(item?.id) {
		segments = item?.let { repository.getSegmentsForItem(it) }.orEmpty()
		Timber.d("Media segments of ${item?.id}: ${segments.map { "${it.type} ${repository.getMediaSegmentAction(it)}" }}")
	}

	val positionInfo by rememberPlayerPositionInfo(playbackManager, precision = 1.seconds)
	val position = positionInfo.active
	val target = askToSkipTarget(segments, repository::getMediaSegmentAction, position)

	// A new segment starts over: shown, not dismissed
	var autoHidden by remember(target) { mutableStateOf(false) }
	var dismissed by remember(target) { mutableStateOf(false) }
	val autoHideDelay = skipAutoHideDelay(preferences[VelellaPreferences.skipAutoHideSeconds])
	LaunchedEffect(target, autoHidden, autoHideDelay) {
		if (target == null || autoHidden || autoHideDelay == null) return@LaunchedEffect
		delay(autoHideDelay)
		autoHidden = true
	}

	val state = if (dismissed) SkipButtonState.NONE else skipButtonState(!controlsVisible, autoHidden, target, position)
	return SkipSegment(state) { action ->
		when (action) {
			SkipKeyAction.SKIP -> {
				target?.let { playbackManager.state.seek(it) }
				dismissed = true
			}
			SkipKeyAction.DISMISS -> dismissed = true
			SkipKeyAction.RECALL -> autoHidden = false
		}
	}
}
