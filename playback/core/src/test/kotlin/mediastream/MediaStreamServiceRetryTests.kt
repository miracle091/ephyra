package org.jellyfin.playback.core.mediastream

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.PlaybackManagerOptions
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.model.PlaybackError
import org.jellyfin.playback.core.model.PlaybackFailure
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.QueueService
import org.jellyfin.playback.core.queue.StreamFallback
import org.jellyfin.playback.core.queue.streamFallback
import org.jellyfin.playback.core.queue.supplier.QueueSupplier
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MediaStreamServiceRetryTests : FunSpec({
	beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
	afterTest { Dispatchers.resetMain() }

	val sourceError = PlaybackError(PlaybackError.Reason.SOURCE, "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED")

	class Player(
		val backend: PlayerBackend,
		val events: PlayerBackendEventListener,
		val failures: List<PlaybackFailure>,
		val asked: List<StreamFallback?>,
		val scope: CoroutineScope,
		val entry: QueueEntry,
	)

	// One entry; the resolver records the fallback each request asks for and gives a stream while [streams] allows
	fun createPlayer(streams: (Int) -> Boolean = { true }): Player {
		val entry = QueueEntry()
		val listener = slot<PlayerBackendEventListener>()
		val backend = mockk<PlayerBackend>(relaxed = true) {
			every { setListener(capture(listener)) } returns Unit
			every { getPositionInfo() } returns PositionInfo.EMPTY
		}
		val asked = CopyOnWriteArrayList<StreamFallback?>()
		val resolver = object : MediaStreamResolver {
			override suspend fun getStream(queueEntry: QueueEntry): PlayableMediaStream? {
				asked += queueEntry.streamFallback
				if (!streams(asked.size)) return null
				return PlayableMediaStream(
					identifier = "stream-${asked.size}",
					conversionMethod = MediaConversionMethod.None,
					container = MediaStreamContainer("mkv"),
					tracks = emptyList(),
					queueEntry = queueEntry,
					url = "http://server/stream-${asked.size}",
				)
			}
		}
		val queue = QueueService()
		val service = MediaStreamService(listOf(resolver), Duration.ZERO)
		val options = PlaybackManagerOptions(mockk(relaxed = true), { Duration.ZERO }, { Duration.ZERO })
		PlaybackManager(backend, mutableListOf(queue, service), options)

		val failures = CopyOnWriteArrayList<PlaybackFailure>()
		val scope = CoroutineScope(Dispatchers.Unconfined)
		scope.launch { service.failures.collect { failures += it } }
		queue.addSupplier(object : QueueSupplier {
			override val size = 1
			override suspend fun getItem(index: Int): QueueEntry? = entry.takeIf { index == 0 }
		})
		return Player(backend, listener.captured, failures, asked, scope, entry)
	}

	test("a stream error plays the entry again from a stream without direct play") {
		val player = createPlayer()
		val entry = player.entry
		eventually(2.seconds) { verify(exactly = 1) { player.backend.playItem(entry) } }

		player.events.onPlayError(sourceError)

		eventually(2.seconds) { verify(exactly = 2) { player.backend.playItem(entry) } }
		player.asked shouldContainExactly listOf(null, StreamFallback.NO_DIRECT_PLAY)
		player.failures shouldContainExactly listOf(PlaybackFailure.Retrying(sourceError, StreamFallback.NO_DIRECT_PLAY))
		player.scope.cancel()
	}

	test("the third error in a row gives up after a transcode") {
		val player = createPlayer()
		val entry = player.entry
		eventually(2.seconds) { verify(exactly = 1) { player.backend.playItem(entry) } }

		player.events.onPlayError(sourceError)
		eventually(2.seconds) { verify(exactly = 2) { player.backend.playItem(entry) } }
		player.events.onPlayError(sourceError)
		eventually(2.seconds) { verify(exactly = 3) { player.backend.playItem(entry) } }
		player.events.onPlayError(sourceError)

		eventually(2.seconds) { player.failures.size shouldBe 3 }
		player.asked shouldContainExactly listOf(null, StreamFallback.NO_DIRECT_PLAY, StreamFallback.TRANSCODE)
		player.failures shouldContainExactly listOf(
			PlaybackFailure.Retrying(sourceError, StreamFallback.NO_DIRECT_PLAY),
			PlaybackFailure.Retrying(sourceError, StreamFallback.TRANSCODE),
			PlaybackFailure.GaveUp(sourceError),
		)
		verify(exactly = 3) { player.backend.playItem(entry) }
		player.scope.cancel()
	}

	test("a retry without a new stream gives up and keeps the current one") {
		val player = createPlayer(streams = { request -> request == 1 })
		val entry = player.entry
		eventually(2.seconds) { verify(exactly = 1) { player.backend.playItem(entry) } }

		player.events.onPlayError(sourceError)

		eventually(2.seconds) { player.failures.size shouldBe 2 }
		player.failures shouldContainExactly listOf(
			PlaybackFailure.Retrying(sourceError, StreamFallback.NO_DIRECT_PLAY),
			PlaybackFailure.GaveUp(sourceError),
		)
		verify(exactly = 1) { player.backend.playItem(entry) }
		player.scope.cancel()
	}
})
