package org.jellyfin.androidtv.ui.player.video

import androidx.annotation.DrawableRes
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.player.base.toast.MediaToastRegistry
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState

/**
 * Velella: a play or pause asked for by the user, with the icon shown in the middle of the screen for it. The icon comes from the
 * user's command, not from the player's state, so it never shows when playback starts, buffers or seeks by itself (the player
 * counts buffering as paused). Only a resume shows one: a pause shows the controls instead, and a start from stopped nothing.
 */
enum class PlayPauseCommand(@param:DrawableRes val icon: Int?) {
	PLAY(null),
	PAUSE(null),
	UNPAUSE(R.drawable.ic_play),
}

/** Velella: what the play/pause button and key do in [state]: pause while playing, go on from a pause, start otherwise. */
fun playPauseCommand(state: PlayState): PlayPauseCommand = when (state) {
	PlayState.PLAYING -> PlayPauseCommand.PAUSE
	PlayState.PAUSED -> PlayPauseCommand.UNPAUSE
	PlayState.STOPPED, PlayState.ERROR -> PlayPauseCommand.PLAY
}

/** Velella: runs the user's [command] and shows its icon. */
fun PlaybackManager.perform(command: PlayPauseCommand, mediaToastRegistry: MediaToastRegistry) {
	when (command) {
		PlayPauseCommand.PLAY -> state.play()
		PlayPauseCommand.PAUSE -> state.pause()
		PlayPauseCommand.UNPAUSE -> state.unpause()
	}
	command.icon?.let { mediaToastRegistry.emit(it) }
}
