package org.jellyfin.playback.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.jellyfin.playback.core.backend.BackendService
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackError
import kotlin.time.Duration

class PlayerStateErrorTests : FunSpec({
	fun createState(): Pair<PlayerState, PlayerBackendEventListener> {
		val listener = slot<PlayerBackendEventListener>()
		val backend = mockk<PlayerBackend>(relaxed = true) {
			every { setListener(capture(listener)) } returns Unit
		}
		val backendService = BackendService().apply { switchBackend(backend) }
		val options = PlaybackManagerOptions(mockk(relaxed = true), { Duration.ZERO }, { Duration.ZERO })
		return MutablePlayerState(options, backendService, null) to listener.captured
	}

	val decoderError = PlaybackError(PlaybackError.Reason.DECODER, "ERROR_CODE_DECODER_INIT_FAILED: no decoder")

	test("the backend's error is exposed when playback fails") {
		val (state, backend) = createState()

		backend.onPlayError(decoderError)
		backend.onPlayStateChange(PlayState.ERROR)

		state.playState.value shouldBe PlayState.ERROR
		state.error.value shouldBe decoderError
	}

	test("the error stays while paused or stopped and clears once playback runs again") {
		val (state, backend) = createState()
		backend.onPlayError(decoderError)
		backend.onPlayStateChange(PlayState.ERROR)

		backend.onPlayStateChange(PlayState.STOPPED)
		state.error.value shouldBe decoderError

		backend.onPlayStateChange(PlayState.PLAYING)
		state.error.value shouldBe null
	}
})
