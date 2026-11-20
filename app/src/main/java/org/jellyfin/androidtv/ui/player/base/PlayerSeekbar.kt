package org.jellyfin.androidtv.ui.player.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jellyfin.androidtv.ui.base.Seekbar
import org.jellyfin.androidtv.ui.base.SeekbarCallbacks
import org.jellyfin.androidtv.ui.base.SeekbarColors
import org.jellyfin.androidtv.ui.base.SeekbarDefaults
import org.jellyfin.androidtv.ui.base.SeekbarPosition
import org.jellyfin.androidtv.ui.base.SeekbarStyle
import org.jellyfin.androidtv.ui.composable.rememberPlayerProgress
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.koin.compose.koinInject
import kotlin.time.Duration
import kotlin.time.times

@Composable
fun PlayerSeekbar(
	modifier: Modifier = Modifier,
	colors: SeekbarColors = SeekbarDefaults.colors(),
	playbackManager: PlaybackManager = koinInject<PlaybackManager>(),
	// Ephyra: the position picked while scrubbing with the arrows, null when not scrubbing (for the trickplay thumbnail)
	onScrubPositionChange: ((Duration?) -> Unit)? = null,
	// Ephyra: the video player keeps a smaller knob without the focus, like the legacy one
	knobAlwaysVisible: Boolean = false,
) {
	val playState by playbackManager.state.playState.collectAsState()
	val positionInfo = playbackManager.state.positionInfo
	val progress by rememberPlayerProgress(
		playing = playState == PlayState.PLAYING,
		active = positionInfo.active,
		duration = positionInfo.duration,
	)
	val seekForwardAmount = remember { playbackManager.options.defaultFastForwardAmount() }
	val seekRewindAmount = remember { playbackManager.options.defaultRewindAmount() }

	Seekbar(
		position = SeekbarPosition(
			progress = progress.toDouble() * positionInfo.duration,
			buffer = positionInfo.buffer,
			duration = positionInfo.duration,
			seekForwardAmount = seekForwardAmount,
			seekRewindAmount = seekRewindAmount,
		),
		callbacks = SeekbarCallbacks(
			onScrubbing = { scrubbing ->
				playbackManager.state.setScrubbing(scrubbing)
				if (!scrubbing) onScrubPositionChange?.invoke(null)
			},
			onSeek = { progress -> playbackManager.state.seek(progress) },
			onScrub = { progress -> onScrubPositionChange?.invoke(progress) },
		),
		modifier = modifier,
		enabled = positionInfo.duration > Duration.ZERO,
		style = SeekbarStyle(colors, knobAlwaysVisible),
	)
}
