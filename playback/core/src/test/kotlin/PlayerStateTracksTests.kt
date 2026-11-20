package org.jellyfin.playback.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.jellyfin.playback.core.backend.BackendService
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.model.SelectedTracks
import kotlin.time.Duration

class PlayerStateTracksTests : FunSpec({
	fun createState(): Pair<PlayerState, PlayerBackendEventListener> {
		val listener = slot<PlayerBackendEventListener>()
		val backend = mockk<PlayerBackend>(relaxed = true) {
			every { setListener(capture(listener)) } returns Unit
		}
		val backendService = BackendService().apply { switchBackend(backend) }
		val options = PlaybackManagerOptions(mockk(relaxed = true), { Duration.ZERO }, { Duration.ZERO })
		return MutablePlayerState(options, backendService, null) to listener.captured
	}

	test("nothing is selected before playback") {
		createState().first.selectedTracks.value shouldBe SelectedTracks.NONE
	}

	test("the tracks the backend shows are exposed, also after a later pick") {
		val (state, backend) = createState()

		backend.onSelectedTracksChange(SelectedTracks(audioIndex = 1, subtitleIndex = null))
		state.selectedTracks.value shouldBe SelectedTracks(audioIndex = 1, subtitleIndex = null)

		backend.onSelectedTracksChange(SelectedTracks(audioIndex = 1, subtitleIndex = 4))
		state.selectedTracks.value shouldBe SelectedTracks(audioIndex = 1, subtitleIndex = 4)
	}

	test("stopping forgets the tracks") {
		val (state, backend) = createState()
		backend.onSelectedTracksChange(SelectedTracks(audioIndex = 1, subtitleIndex = 4))

		state.stop()

		state.selectedTracks.value shouldBe SelectedTracks.NONE
	}
})
