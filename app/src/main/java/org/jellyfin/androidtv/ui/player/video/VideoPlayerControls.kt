package org.jellyfin.androidtv.ui.player.video

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.integration.dream.formatPlaybackTime
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.SeekbarDefaults
import org.jellyfin.androidtv.ui.base.SeekbarStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.base.popover.Popover
import org.jellyfin.androidtv.ui.base.popover.PopoverPlacement
import org.jellyfin.androidtv.ui.composable.rememberPlayerPositionInfo
import org.jellyfin.androidtv.ui.player.base.PlayerSeekbar
import org.jellyfin.androidtv.util.getTimeFormatter
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.mediastream.mediaStreamFlow
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.baseItemFlow
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.queue
import org.koin.compose.koinInject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import java.time.LocalTime

private val PREVIEW_HIDE_DELAY = 1.5.seconds
private val SEEKBAR_HEIGHT = 3.dp
private val SEEKBAR_FOCUSED_HEIGHT = 6.dp
private val SEEKBAR_KNOB_RADIUS = 8.dp
// Velella: a little larger than the other icons, so the seconds inside read from across the room
private val SKIP_ICON_SIZE = 30.dp
private val ICON_SIZE = 24.dp

/** Velella: the seek bar in its 24dp row, with the trickplay thumbnail of the picked position while scrubbing, as in the legacy player. */
@Composable
private fun VideoSeekbar(
	playbackManager: PlaybackManager,
	focusRequester: FocusRequester,
	downFocusRequester: FocusRequester,
) = Box(
	contentAlignment = Alignment.Center,
	modifier = Modifier
		.fillMaxWidth()
		.height(24.dp),
) {
	var scrubPosition by remember { mutableStateOf<Duration?>(null) }
	val previewScope = rememberCoroutineScope()
	var previewHideJob by remember { mutableStateOf<Job?>(null) }
	// Velella: a thin line while the buttons have the focus, thicker when it is the one moving
	var focused by remember { mutableStateOf(false) }
	val barHeight by animateDpAsState(if (focused) SEEKBAR_FOCUSED_HEIGHT else SEEKBAR_HEIGHT)
	PlayerSeekbar(
		playbackManager = playbackManager,
		style = SeekbarStyle(SeekbarDefaults.colors(), knobAlwaysVisible = true, knobRadius = SEEKBAR_KNOB_RADIUS),
		onScrubPositionChange = { position ->
			previewHideJob?.cancel()
			// Keep the thumbnail a moment after the last press, so it doesn't blink between presses
			if (position != null) scrubPosition = position
			else previewHideJob = previewScope.launch {
				delay(PREVIEW_HIDE_DELAY)
				scrubPosition = null
			}
		},
		modifier = Modifier
			.fillMaxWidth()
			.height(barHeight)
			.onFocusChanged { focused = it.isFocused }
			// Velella: down from the seek bar goes to play/pause; the 2D focus search picked a button by distance (Next)
			.onPreviewKeyEvent { event ->
				val down = event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown
				if (down) downFocusRequester.requestFocus()
				down
			}
			.focusRequester(focusRequester)
	)

	val position = scrubPosition
	val duration = playbackManager.state.positionInfo.duration
	if (position != null && duration > Duration.ZERO) {
		val entry by rememberQueueEntry(playbackManager)
		val item = entry?.run { baseItemFlow.collectAsState(baseItem) }?.value
		val stream = entry?.run { mediaStreamFlow.collectAsState(mediaStream) }?.value
		TrickplayPreview(
			item = item,
			mediaSourceId = stream?.mediaSourceId,
			position = position,
			fraction = (position / duration).toFloat(),
			modifier = Modifier.align(Alignment.TopStart),
		)
	}
}

@Composable
fun VideoPlayerControls(
	playbackManager: PlaybackManager = koinInject(),
	onPlaybackInfoClick: () -> Unit = {},
	// Velella: a menu asked for by its remote key (Captions, Audio track), opened once the controls show
	menuRequest: VideoPlayerKeyAction? = null,
	onMenuRequestHandled: () -> Unit = {},
	// Velella: a live channel shows its programme block in place of the seek bar and can't seek, as in the legacy player
	live: LiveControls? = null,
) {
	val playState by playbackManager.state.playState.collectAsState()
	val seekbarFocus = remember { FocusRequester() }
	val playPauseFocus = remember { FocusRequester() }

	// Velella: as in the legacy player, the seek bar comes first in a 24dp row, the times and the end time 8dp under it, then the
	// buttons 16dp lower, their centres 56dp apart (the legacy 48dp buttons with 8dp gaps; these are 44dp, so 12dp gaps and 2dp
	// more around the row). The legacy order: rewind, play/pause, fast forward, next
	Column(
		verticalArrangement = Arrangement.Bottom,
	) {
		if (live != null) {
			LiveProgramBlock(live.info, live.playInfo)
		} else {
			VideoSeekbar(playbackManager, seekbarFocus, playPauseFocus)

			PositionRow(playbackManager, Modifier.padding(top = 8.dp))
		}

		Row(
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			modifier = Modifier
				.padding(start = 2.dp, top = 18.dp, end = 2.dp)
				// Velella: up from the buttons goes to the seek bar above them; the 2D focus search skipped the thin bar and left
				// the controls, which hides the overlay. A live channel's bar takes no focus: up leaves and hides the overlay.
				.onPreviewKeyEvent { event ->
					val up = live == null && event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp
					if (up) seekbarFocus.requestFocus()
					up
				}
				.focusRestorer()
				.focusGroup()
		) {
			PreviousEntryButton(playbackManager)
			if (live == null) RewindButton(playbackManager)
			PlayPauseButton(playbackManager, playState, playPauseFocus)
			if (live == null) FastForwardButton(playbackManager)
			NextEntryButton(playbackManager)
			if (live != null) LiveTvPills(live.info, live.actions)

			Spacer(Modifier.weight(1f))

			if (live != null) PreviousChannelButton(live.info.channel.id, live.actions.onSwitch)

			VideoPlayerMenus(playbackManager, menuRequest, onMenuRequestHandled, showQuality = live == null)
			PlaybackInfoButton(onClick = onPlaybackInfoClick)
		}
	}
}

@Composable
private fun PlayPauseButton(
	playbackManager: PlaybackManager,
	playState: PlayState,
	focusRequester: FocusRequester,
) {
	// Velella: take the focus as soon as the controls appear. onVisibilityChanged waited until the button was fully on screen, at
	// the end of the controls' slide in, so a right press during the slide found no focused button and was lost
	LaunchedEffect(focusRequester) { focusRequester.requestFocus() }

	IconButton(
		onClick = {
			when (playState) {
				PlayState.STOPPED,
				PlayState.ERROR -> playbackManager.state.play()

				PlayState.PLAYING -> playbackManager.state.pause()
				PlayState.PAUSED -> playbackManager.state.unpause()
			}
		},
		modifier = Modifier.focusRequester(focusRequester)
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
	val amount = remember { playbackManager.options.defaultRewindAmount() }
	SkipIcon(R.drawable.ic_skip_back, skipSecondsLabel(amount), stringResource(R.string.rewind))
}

@Composable
private fun FastForwardButton(
	playbackManager: PlaybackManager,
) = IconButton(
	onClick = { playbackManager.state.fastForward() },
) {
	val amount = remember { playbackManager.options.defaultFastForwardAmount() }
	SkipIcon(R.drawable.ic_skip_forward, skipSecondsLabel(amount), stringResource(R.string.fast_forward))
}

/** Velella: a circular arrow with the skip length in seconds inside, so the button says how far it jumps. */
@Composable
private fun SkipIcon(@DrawableRes icon: Int, seconds: String, contentDescription: String) = Box(
	contentAlignment = Alignment.Center,
	// The button keeps the size of the others; the larger icon reaches into its padding
	modifier = Modifier.size(ICON_SIZE),
) {
	Icon(
		imageVector = ImageVector.vectorResource(icon),
		contentDescription = contentDescription,
		modifier = Modifier.requiredSize(SKIP_ICON_SIZE),
	)
	Text(
		text = seconds,
		style = LocalTextStyle.current.merge(fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp),
		// The arrow's circle sits a little under the icon's centre
		modifier = Modifier.offset(y = 1.dp),
	)
}

@Composable
private fun PreviousEntryButton(
	playbackManager: PlaybackManager,
) {
	val entryIndex by playbackManager.queue.entryIndex.collectAsState()
	val coroutineScope = rememberCoroutineScope()
	// Velella: as in the legacy player, only when there is a previous entry
	if (entryIndex <= 0) return

	IconButton(
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
	// Velella: as in the legacy player, only when there is a next entry
	if (entryIndex >= playbackManager.queue.estimatedSize - 1) return

	IconButton(
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

/** Velella: the position on the left, the end time and the time left ("-24:20") on the right, under the seek bar. */
@Composable
private fun PositionRow(
	playbackManager: PlaybackManager,
	modifier: Modifier = Modifier,
) {
	val positionInfo by rememberPlayerPositionInfo(playbackManager, precision = 1.seconds)
	val speed by playbackManager.state.speed.collectAsState()
	if (positionInfo.duration == Duration.ZERO) return

	val context = LocalContext.current
	val palette = LocalVelellaPalette.current
	val style = LocalTextStyle.current.merge(color = palette.inkMuted, fontSize = 14.sp, fontFeatureSettings = "tnum")
	val endsAt = context.getTimeFormatter().format(videoEndsAt(positionInfo.active, positionInfo.duration, speed, LocalTime.now()))

	Row(
		horizontalArrangement = Arrangement.spacedBy(24.dp),
		modifier = modifier.fillMaxWidth(),
	) {
		Text(formatPlaybackTime(positionInfo.active.inWholeMilliseconds), style = style)
		Spacer(Modifier.weight(1f))
		Text(stringResource(R.string.lbl_playback_control_ends, endsAt), style = style)
		Text(remainingPlaybackTime(positionInfo.active, positionInfo.duration), style = style)
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
