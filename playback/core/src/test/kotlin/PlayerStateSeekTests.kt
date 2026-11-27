package org.jellyfin.playback.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.playback.core.backend.BackendService
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.QueueService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class PlayerStateSeekTests : FunSpec({
	test("a converted stream the player can't seek in is asked for again at the new position") {
		needsReloadToSeek(seekable = false, conversion = MediaConversionMethod.Transcode, duration = 90.minutes) shouldBe true
		needsReloadToSeek(seekable = false, conversion = MediaConversionMethod.Remux, duration = 90.minutes) shouldBe true
	}

	test("a seekable stream, a direct play or a stream of unknown length (live) is seeked in the player") {
		needsReloadToSeek(seekable = true, conversion = MediaConversionMethod.Transcode, duration = 90.minutes) shouldBe false
		needsReloadToSeek(seekable = false, conversion = MediaConversionMethod.None, duration = 90.minutes) shouldBe false
		needsReloadToSeek(seekable = false, conversion = null, duration = 90.minutes) shouldBe false
		needsReloadToSeek(seekable = false, conversion = MediaConversionMethod.Transcode, duration = Duration.ZERO) shouldBe false
	}

	fun createState(seekable: Boolean, conversion: MediaConversionMethod, reloads: MutableList<Duration>): Pair<PlayerState, PlayerBackend> {
		val backend = mockk<PlayerBackend>(relaxed = true) {
			every { isSeekable } returns seekable
			every { getPositionInfo() } returns PositionInfo(active = 10.minutes, buffer = 10.minutes, duration = 90.minutes)
		}
		val backendService = BackendService().apply { switchBackend(backend) }
		val entry = QueueEntry().apply {
			mediaStream = mockk<PlayableMediaStream> { every { conversionMethod } returns conversion }
		}
		val queue = mockk<QueueService>(relaxed = true) { every { this@mockk.entry } returns MutableStateFlow(entry) }
		val options = PlaybackManagerOptions(mockk(relaxed = true), { 1.minutes }, { 1.minutes })
		return MutablePlayerState(options, backendService, queue, seekByReload = { reloads += it }) to backend
	}

	test("fast forward in a transcode that can't seek restarts the stream there instead of seeking in vain") {
		val reloads = mutableListOf<Duration>()
		val (state, backend) = createState(seekable = false, conversion = MediaConversionMethod.Transcode, reloads)

		state.fastForward()

		reloads shouldBe listOf(11.minutes)
		verify(exactly = 0) { backend.seekTo(any()) }
	}

	test("a seekable stream seeks in the player") {
		val reloads = mutableListOf<Duration>()
		val (state, backend) = createState(seekable = true, conversion = MediaConversionMethod.Transcode, reloads)

		state.seek(30.minutes)

		reloads shouldBe emptyList()
		verify { backend.seekTo(30.minutes) }
	}
})
