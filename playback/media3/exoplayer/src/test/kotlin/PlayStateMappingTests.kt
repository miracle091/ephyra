package org.jellyfin.playback.media3.exoplayer

import androidx.media3.common.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.core.model.PlayState

class PlayStateMappingTests : FunSpec({
	test("scrubbing while playing is still playing") {
		playStateOf(
			isPlaying = false,
			playbackState = Player.STATE_READY,
			playWhenReady = true,
			suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_SCRUBBING,
		) shouldBe PlayState.PLAYING
	}

	test("scrubbing while paused stays paused") {
		playStateOf(
			isPlaying = false,
			playbackState = Player.STATE_READY,
			playWhenReady = false,
			suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_SCRUBBING,
		) shouldBe PlayState.PAUSED
	}

	test("a pause, an end and playback map as before") {
		playStateOf(true, Player.STATE_READY, true, Player.PLAYBACK_SUPPRESSION_REASON_NONE) shouldBe PlayState.PLAYING
		playStateOf(false, Player.STATE_READY, false, Player.PLAYBACK_SUPPRESSION_REASON_NONE) shouldBe PlayState.PAUSED
		playStateOf(false, Player.STATE_ENDED, true, Player.PLAYBACK_SUPPRESSION_REASON_NONE) shouldBe PlayState.STOPPED
		playStateOf(false, Player.STATE_IDLE, false, Player.PLAYBACK_SUPPRESSION_REASON_NONE) shouldBe PlayState.STOPPED
		// Buffering after a seek, or audio focus lost for a moment, is not playing
		playStateOf(false, Player.STATE_BUFFERING, true, Player.PLAYBACK_SUPPRESSION_REASON_NONE) shouldBe PlayState.PAUSED
		playStateOf(
			false,
			Player.STATE_READY,
			true,
			Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS,
		) shouldBe PlayState.PAUSED
	}
})
