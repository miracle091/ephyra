package org.jellyfin.playback.core.mediastream

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
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
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.QueueService
import org.jellyfin.playback.core.queue.supplier.QueueSupplier
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MediaStreamServiceMovedOnTests : FunSpec({
	beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
	afterTest { Dispatchers.resetMain() }

	test("a stream that comes after the queue moved on (another channel, Back) does not play and is discarded") {
		val entry = QueueEntry()
		val backend = mockk<PlayerBackend>(relaxed = true) {
			every { getPositionInfo() } returns PositionInfo.EMPTY
		}
		val asked = CompletableDeferred<Unit>()
		val answer = CompletableDeferred<PlayableMediaStream>()
		val resolver = object : MediaStreamResolver {
			override suspend fun getStream(queueEntry: QueueEntry): PlayableMediaStream {
				asked.complete(Unit)
				return answer.await()
			}
		}
		val queue = QueueService()
		val service = MediaStreamService(listOf(resolver), Duration.ZERO)
		val options = PlaybackManagerOptions(mockk(relaxed = true), { Duration.ZERO }, { Duration.ZERO })
		PlaybackManager(backend, mutableListOf(queue, service), options)

		val discarded = CopyOnWriteArrayList<PlayableMediaStream>()
		val scope = CoroutineScope(Dispatchers.Unconfined)
		scope.launch { service.discardedStreams.collect { discarded += it } }
		queue.addSupplier(object : QueueSupplier {
			override val size = 1
			override suspend fun getItem(index: Int): QueueEntry? = entry.takeIf { index == 0 }
		})
		eventually(2.seconds) { asked.isCompleted shouldBe true }

		queue.clear()
		val stream = PlayableMediaStream(
			identifier = "live",
			conversionMethod = MediaConversionMethod.Transcode,
			container = MediaStreamContainer("ts"),
			tracks = emptyList(),
			queueEntry = entry,
			url = "http://server/live",
			liveStreamId = "tuner-1",
		)
		answer.complete(stream)

		eventually(2.seconds) { discarded shouldContainExactly listOf(stream) }
		verify(exactly = 0) { backend.playItem(entry) }
		scope.cancel()
	}
})
