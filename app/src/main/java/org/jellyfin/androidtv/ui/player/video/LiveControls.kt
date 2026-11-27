package org.jellyfin.androidtv.ui.player.video

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.PlayInfo
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.androidtv.ui.player.base.PlayerOverlayVisibilityState
import org.koin.compose.koinInject

/**
 * Velella: what the video player shows and does for a live TV channel: its programmes ([info]), how the server delivers the
 * stream ([playInfo]) and the actions of its buttons.
 */
class LiveControls(
	val info: LiveChannelInfo,
	val playInfo: String?,
	val actions: LiveTvActions,
)

/**
 * Velella: the live player's own keys, before the video player's: the open channel bar takes all of them (false), and down with
 * the controls hidden opens it, as in the legacy player (true). Null passes the key on.
 */
fun liveOverlayKey(keyCode: Int, live: LiveControls?, channelBarOpen: Boolean, controlsVisible: Boolean): Boolean? = when {
	channelBarOpen -> false
	live != null && !controlsVisible && keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
		live.actions.onChannels()
		true
	}
	else -> null
}

/**
 * Velella: this key action, or null for a seek while a [live] channel plays: it can't seek, as in the legacy player, so its seek
 * keys are left to the overlay (left and right show the controls).
 */
fun VideoPlayerKeyAction.unlessSeekingLive(live: LiveControls?): VideoPlayerKeyAction? = takeUnless { live != null && seeks }

/**
 * Velella: the live controls of [item] when it is a live TV channel, null for anything else. Channels opens the [channelBar]; a
 * channel is tuned with [liveTvSwitch] after the controls hide (the previous channel button leaves while the queue is rebuilt, and
 * the overlay takes the focus).
 */
@Composable
fun rememberLiveControls(
	item: BaseItemDto?,
	stream: PlayableMediaStream?,
	visibilityState: PlayerOverlayVisibilityState,
	channelBar: ChannelBarState,
	liveTvSwitch: LiveTvSwitch?,
): LiveControls? {
	val navigationRepository = koinInject<NavigationRepository>()
	var refresh by remember { mutableIntStateOf(0) }
	val info = rememberLiveChannelInfo(item, refresh)
	val toggleRecording = rememberLiveRecordingToggle(onChanged = { refresh++ })
	val context = LocalContext.current
	val playInfo = remember(stream) { stream?.let { PlayInfo.describe(context, it.conversionMethod.playMethod, it.url) } }
	if (info == null) return null

	return LiveControls(
		info = info,
		playInfo = playInfo,
		actions = LiveTvActions(
			// The guide screen in the player's place (a player left in the history would come back with an empty queue): picking a
			// channel there plays it
			onGuide = { navigationRepository.navigate(Destinations.liveTvGuide, replace = true) },
			onRecord = { info.program?.let(toggleRecording) },
			onChannels = {
				visibilityState.hide()
				channelBar.open()
			},
			onSwitch = { channelId ->
				visibilityState.hide()
				liveTvSwitch?.switchChannel?.invoke(channelId)
			},
		),
	)
}
