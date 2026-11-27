package org.jellyfin.playback.core.queue

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.PlaybackManagerOptions
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.queue.supplier.QueueSupplier
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class QueueAdvanceAtEndTests : FunSpec({
	class Entries(override val size: Int) : QueueSupplier {
		val items = List(size) { QueueEntry() }
		override suspend fun getItem(index: Int): QueueEntry? = items.getOrNull(index)
	}

	fun createQueue(supplier: Entries): Triple<QueueService, PlayerBackendEventListener, PlaybackManager> {
		val listener = slot<PlayerBackendEventListener>()
		val backend = mockk<PlayerBackend>(relaxed = true) {
			every { setListener(capture(listener)) } returns Unit
		}
		val queue = QueueService()
		val options = PlaybackManagerOptions(mockk(relaxed = true), { Duration.ZERO }, { Duration.ZERO })
		val manager = PlaybackManager(backend, mutableListOf(queue), options)
		queue.addSupplier(supplier)
		return Triple(queue, listener.captured, manager)
	}

	test("by default the queue moves on when an entry ends") {
		val supplier = Entries(2)
		val (queue, backend) = createQueue(supplier)
		eventually(2.seconds) { queue.entry.value shouldBe supplier.items[0] }

		backend.onMediaStreamEnd(mockk(relaxed = true))

		eventually(2.seconds) { queue.entry.value shouldBe supplier.items[1] }
		queue.heldNext.value shouldBe null
	}

	test("a held end keeps the entry and offers the next one until the queue moves on") {
		val supplier = Entries(2)
		val (queue, backend) = createQueue(supplier)
		eventually(2.seconds) { queue.entry.value shouldBe supplier.items[0] }
		val asked = mutableListOf<Pair<QueueEntry, QueueEntry>>()
		queue.advanceAtEnd = { ended, next -> asked += ended to next; false }

		backend.onMediaStreamEnd(mockk(relaxed = true))

		eventually(2.seconds) { queue.heldNext.value shouldBe supplier.items[1] }
		queue.entry.value shouldBe supplier.items[0]
		asked shouldBe listOf(supplier.items[0] to supplier.items[1])

		runBlocking { queue.next(usePlaybackOrder = true, useRepeatMode = false) }
		queue.entry.value shouldBe supplier.items[1]
		queue.heldNext.value shouldBe null
	}

	test("the last entry ends the queue without asking") {
		val supplier = Entries(1)
		val (queue, backend) = createQueue(supplier)
		eventually(2.seconds) { queue.entry.value shouldBe supplier.items[0] }
		queue.advanceAtEnd = { _, _ -> error("not asked without a next entry") }

		backend.onMediaStreamEnd(mockk(relaxed = true))

		eventually(2.seconds) { queue.entry.value shouldBe null }
		queue.heldNext.value shouldBe null
	}
})
