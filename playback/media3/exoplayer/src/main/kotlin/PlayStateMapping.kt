package org.jellyfin.playback.media3.exoplayer

import androidx.media3.common.Player
import org.jellyfin.playback.core.model.PlayState

/**
 * Ephyra: the [PlayState] of a Media3 player. Scrubbing mode holds the frames while the user picks a position on the seek bar: it
 * is no pause, so the player still counts as playing (no pause toast, no play icon while scrubbing).
 */
internal fun playStateOf(
	isPlaying: Boolean,
	@Player.State playbackState: Int,
	playWhenReady: Boolean,
	@Player.PlaybackSuppressionReason suppressionReason: Int,
): PlayState = when {
	isPlaying -> PlayState.PLAYING
	playWhenReady && suppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_SCRUBBING -> PlayState.PLAYING
	playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED -> PlayState.STOPPED
	else -> PlayState.PAUSED
}
