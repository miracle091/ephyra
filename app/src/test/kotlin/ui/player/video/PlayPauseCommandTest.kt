package org.jellyfin.androidtv.ui.player.video

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.player.base.toast.MediaToastRegistry
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState

class PlayPauseCommandTest : FunSpec({
	test("Play/pause pauses while playing, goes on from a pause and starts otherwise") {
		playPauseCommand(PlayState.PLAYING) shouldBe PlayPauseCommand.PAUSE
		playPauseCommand(PlayState.PAUSED) shouldBe PlayPauseCommand.UNPAUSE
		playPauseCommand(PlayState.STOPPED) shouldBe PlayPauseCommand.PLAY
		playPauseCommand(PlayState.ERROR) shouldBe PlayPauseCommand.PLAY
	}

	// Velella: the icon in the middle of the screen comes from the user's command only, never from the player starting or buffering
	test("Only a resume by the user shows its icon; a pause shows the controls instead and a start nothing") {
		val playbackManager = mockk<PlaybackManager>(relaxed = true)
		val toasts = mockk<MediaToastRegistry>(relaxed = true)
		every { playbackManager.state } returns mockk(relaxed = true)

		playbackManager.perform(PlayPauseCommand.PAUSE, toasts)
		playbackManager.perform(PlayPauseCommand.UNPAUSE, toasts)
		playbackManager.perform(PlayPauseCommand.PLAY, toasts)

		verify(exactly = 1) { toasts.emit(R.drawable.ic_play) }
		verify(exactly = 1) { toasts.emit(any<Int>()) }
	}
})
