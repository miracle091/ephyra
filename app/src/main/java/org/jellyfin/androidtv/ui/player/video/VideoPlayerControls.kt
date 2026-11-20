package org.jellyfin.androidtv.ui.player.video

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.integration.dream.formatPlaybackTime
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.base.popover.Popover
import org.jellyfin.androidtv.ui.base.popover.PopoverPlacement
import org.jellyfin.androidtv.ui.composable.rememberPlayerPositionInfo
import org.jellyfin.androidtv.ui.player.base.PlayerSeekbar
import org.jellyfin.androidtv.util.getTimeFormatter
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.queue
import org.koin.compose.koinInject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import java.time.LocalTime

@Composable
fun VideoPlayerControls(
	playbackManager: PlaybackManager = koinInject(),
	onPlaybackInfoClick: () -> Unit = {},
	modifier: Modifier = Modifier,
) {
	val playState by playbackManager.state.playState.collectAsState()

	// Ephyra: as in the legacy player, the seek bar comes first in a 24dp row, the times and the end time 8dp under it, then the
	// buttons 16dp lower, their centres 56dp apart (the legacy 48dp buttons with 8dp gaps; these are 44dp, so 12dp gaps and 2dp
	// more around the row). The legacy order: rewind, play/pause, fast forward, next
	Column(
		verticalArrangement = Arrangement.Bottom,
		modifier = modifier,
	) {
		Box(
			contentAlignment = Alignment.Center,
			modifier = Modifier
				.fillMaxWidth()
				.height(24.dp),
		) {
			PlayerSeekbar(
				playbackManager = playbackManager,
				modifier = Modifier
					.fillMaxWidth()
					.height(4.dp)
			)
		}

		PositionRow(playbackManager, Modifier.padding(top = 8.dp))

		Row(
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			modifier = Modifier
				.padding(start = 2.dp, top = 18.dp, end = 2.dp)
				.focusRestorer()
				.focusGroup()
		) {
			RewindButton(playbackManager)
			PlayPauseButton(playbackManager, playState)
			FastForwardButton(playbackManager)
			NextEntryButton(playbackManager)

			Spacer(Modifier.weight(1f))

			PlaybackInfoButton(onClick = onPlaybackInfoClick)

			MoreOptionsButton {
				PreviousEntryButton(playbackManager)
			}
		}
	}
}

@Composable
private fun PlayPauseButton(
	playbackManager: PlaybackManager,
	playState: PlayState,
) {
	val focusRequester = remember { FocusRequester() }
	IconButton(
		onClick = {
			when (playState) {
				PlayState.STOPPED,
				PlayState.ERROR -> playbackManager.state.play()

				PlayState.PLAYING -> playbackManager.state.pause()
				PlayState.PAUSED -> playbackManager.state.unpause()
			}
		},
		modifier = Modifier
			.focusRequester(focusRequester)
			.onVisibilityChanged {
				focusRequester.requestFocus()
			}
	) {
		AnimatedContent(playState) { playState ->
			when (playState) {
				PlayState.PLAYING -> {
					Icon(
						imageVector = ImageVector.vectorResource(R.drawable.ic_pause),
						contentDescription = stringResource(R.string.lbl_pause),
					)
				}

				PlayState.STOPPED,
				PlayState.PAUSED,
				PlayState.ERROR -> {
					Icon(
						imageVector = ImageVector.vectorResource(R.drawable.ic_play),
						contentDescription = stringResource(R.string.lbl_play),
					)
				}
			}
		}
	}
}

@Composable
private fun RewindButton(
	playbackManager: PlaybackManager,
) = IconButton(
	onClick = { playbackManager.state.rewind() },
) {
	Icon(
		imageVector = ImageVector.vectorResource(R.drawable.ic_rewind),
		contentDescription = stringResource(R.string.rewind),
	)
}

@Composable
private fun FastForwardButton(
	playbackManager: PlaybackManager,
) = IconButton(
	onClick = { playbackManager.state.fastForward() },
) {
	Icon(
		imageVector = ImageVector.vectorResource(R.drawable.ic_fast_forward),
		contentDescription = stringResource(R.string.fast_forward),
	)
}

@Composable
private fun PreviousEntryButton(
	playbackManager: PlaybackManager,
) {
	val entryIndex by playbackManager.queue.entryIndex.collectAsState()
	val coroutineScope = rememberCoroutineScope()

	IconButton(
		enabled = entryIndex > 0,
		onClick = {
			coroutineScope.launch {
				playbackManager.queue.previous()
			}
		},
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_previous),
			contentDescription = stringResource(R.string.lbl_prev_item),
		)
	}
}

@Composable
private fun NextEntryButton(
	playbackManager: PlaybackManager,
) {
	val entryIndex by playbackManager.queue.entryIndex.collectAsState()
	val coroutineScope = rememberCoroutineScope()

	IconButton(
		enabled = entryIndex < playbackManager.queue.estimatedSize - 1,
		onClick = {
			coroutineScope.launch {
				playbackManager.queue.next()
			}
		},
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_next),
			contentDescription = stringResource(R.string.lbl_next_item),
		)
	}
}

private const val MORE_OPTIONS_POPOVER_OFFSET_Y = -5

/** Ephyra: the position on the left, the end time and the duration on the right, under the seek bar as in the legacy player. */
@Composable
private fun PositionRow(
	playbackManager: PlaybackManager,
	modifier: Modifier = Modifier,
) {
	val positionInfo by rememberPlayerPositionInfo(playbackManager, precision = 1.seconds)
	val speed by playbackManager.state.speed.collectAsState()
	if (positionInfo.duration == Duration.ZERO) return

	val context = LocalContext.current
	val palette = LocalEphyraPalette.current
	val style = LocalTextStyle.current.merge(color = palette.inkMuted, fontSize = 14.sp, fontFeatureSettings = "tnum")
	val endsAt = context.getTimeFormatter().format(videoEndsAt(positionInfo.active, positionInfo.duration, speed, LocalTime.now()))

	Row(
		horizontalArrangement = Arrangement.spacedBy(24.dp),
		modifier = modifier.fillMaxWidth(),
	) {
		Text(formatPlaybackTime(positionInfo.active.inWholeMilliseconds), style = style)
		Spacer(Modifier.weight(1f))
		Text(stringResource(R.string.lbl_playback_control_ends, endsAt), style = style)
		Text(formatPlaybackTime(positionInfo.duration.inWholeMilliseconds), style = style)
	}
}

@Composable
private fun MoreOptionsButton(
	content: @Composable () -> Unit,
) = Box {
	var expanded by remember { mutableStateOf(false) }
	IconButton(
		onClick = { expanded = true },
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_more),
			contentDescription = stringResource(R.string.lbl_other_options),
		)
	}

	Popover(
		expanded = expanded,
		onDismissRequest = { expanded = false },
		placement = PopoverPlacement(
			alignment = Alignment.TopCenter,
			offset = DpOffset(0.dp, MORE_OPTIONS_POPOVER_OFFSET_Y.dp),
		),
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			modifier = Modifier
				.padding(4.dp)
		) {
			content()
		}
	}
}

@Composable
fun PlaybackInfoButton(
	onClick: () -> Unit,
) = IconButton(
	onClick = onClick,
) {
	Icon(
		imageVector = ImageVector.vectorResource(R.drawable.ic_info),
		contentDescription = stringResource(R.string.playback_info),
	)
}
