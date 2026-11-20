package org.jellyfin.androidtv.ui.player.video

import android.view.KeyEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jellyfin.androidtv.ui.player.base.toast.MediaToastRegistry
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.PlayerState
import org.jellyfin.playback.core.model.PositionInfo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class QuickSeekKeysTest : FunSpec({
	fun playerStateAt(position: Duration) = mockk<PlayerState>(relaxed = true) {
		every { positionInfo } returns PositionInfo(active = position, buffer = position, duration = 40.minutes)
	}

	fun playbackManagerWith(playerState: PlayerState) = mockk<PlaybackManager>(relaxed = true) {
		every { state } returns playerState
		every { options.defaultFastForwardAmount } returns { 30.seconds }
		every { options.defaultRewindAmount } returns { 10.seconds }
	}

	test("presses add up to one seek, which centre makes at once instead of pausing") {
		val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
		val playerState = playerStateAt(5.minutes)
		val quickSeek = QuickSeek(playbackManagerWith(playerState), MediaToastRegistry(scope), scope)

		quickSeek.onKey(VideoPlayerKeyAction.QUICK_SEEK_FORWARD, KeyEvent.KEYCODE_DPAD_RIGHT) shouldBe true
		quickSeek.onKey(VideoPlayerKeyAction.QUICK_SEEK_FORWARD, KeyEvent.KEYCODE_DPAD_RIGHT) shouldBe true
		quickSeek.onKey(VideoPlayerKeyAction.QUICK_SEEK_REWIND, KeyEvent.KEYCODE_DPAD_LEFT) shouldBe true
		verify(exactly = 0) { playerState.seek(any()) }

		quickSeek.onKey(VideoPlayerKeyAction.PLAY_PAUSE, KeyEvent.KEYCODE_DPAD_CENTER) shouldBe true
		verify(exactly = 1) { playerState.seek(5.minutes + 50.seconds) }
		scope.cancel()
	}

	test("centre and the play/pause key are left to playback when no quick seek waits") {
		val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
		val quickSeek = QuickSeek(playbackManagerWith(playerStateAt(5.minutes)), MediaToastRegistry(scope), scope)

		quickSeek.onKey(VideoPlayerKeyAction.PLAY_PAUSE, KeyEvent.KEYCODE_DPAD_CENTER) shouldBe false
		quickSeek.onKey(VideoPlayerKeyAction.QUICK_SEEK_FORWARD, KeyEvent.KEYCODE_DPAD_RIGHT)
		quickSeek.onKey(VideoPlayerKeyAction.PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) shouldBe false
		scope.cancel()
	}
})
