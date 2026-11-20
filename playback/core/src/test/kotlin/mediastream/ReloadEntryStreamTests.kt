package org.jellyfin.playback.core.mediastream

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.startPosition
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private fun stream(entry: QueueEntry, id: String) = PlayableMediaStream(
	identifier = id,
	conversionMethod = MediaConversionMethod.Transcode,
	container = MediaStreamContainer("ts"),
	tracks = emptyList(),
	queueEntry = entry,
	url = "https://server/$id",
)

class ReloadEntryStreamTests : FunSpec({
	test("a new stream replaces the old one and plays on from the current position") {
		val entry = QueueEntry()
		val old = stream(entry, "old").also { entry.mediaStream = it }
		val new = stream(entry, "new")
		var startAtRequest: Duration? = null
		var played = 0

		val reloaded = runBlocking {
			reloadEntryStream(entry, 12.minutes, isCurrent = { true }, resolve = { startAtRequest = entry.startPosition; new }) { played++ }
		}

		reloaded shouldBe true
		startAtRequest shouldBe 12.minutes
		entry.mediaStream shouldBe new
		entry.startPosition shouldBe 12.minutes
		played shouldBe 1
		entry.mediaStream shouldBe new
		(entry.mediaStream === old) shouldBe false
	}

	test("a failed request keeps the old stream playing") {
		val entry = QueueEntry()
		val old = stream(entry, "old").also { entry.mediaStream = it }
		var played = 0

		val reloaded = runBlocking { reloadEntryStream(entry, 12.minutes, isCurrent = { true }, resolve = { null }) { played++ } }

		reloaded shouldBe false
		entry.mediaStream shouldBe old
		entry.startPosition shouldBe null
		played shouldBe 0
	}

	test("playback that moved on (stop, another entry) is not taken back") {
		val entry = QueueEntry()
		val old = stream(entry, "old").also { entry.mediaStream = it }
		var played = 0

		val reloaded = runBlocking {
			reloadEntryStream(entry, 12.minutes, isCurrent = { false }, resolve = { stream(entry, "new") }) { played++ }
		}

		reloaded shouldBe false
		entry.mediaStream shouldBe old
		entry.startPosition shouldBe null
		played shouldBe 0
	}

	test("a newer stream set meanwhile wins over this reload") {
		val entry = QueueEntry()
		stream(entry, "old").also { entry.mediaStream = it }
		val newer = stream(entry, "newer")
		var played = 0

		val reloaded = runBlocking {
			reloadEntryStream(entry, 12.minutes, isCurrent = { true }, resolve = { entry.mediaStream = newer; stream(entry, "new") }) {
				played++
			}
		}

		reloaded shouldBe false
		entry.mediaStream shouldBe newer
		played shouldBe 0
	}

	test("an entry still waiting for its first stream is left alone") {
		val entry = QueueEntry()
		var asked = 0

		val reloaded = runBlocking { reloadEntryStream(entry, 12.minutes, isCurrent = { true }, resolve = { asked++; null }) {} }

		reloaded shouldBe false
		asked shouldBe 0
	}

	test("a cancelled reload forgets the start position") {
		val entry = QueueEntry()
		stream(entry, "old").also { entry.mediaStream = it }

		shouldThrow<CancellationException> {
			runBlocking {
				reloadEntryStream(entry, 12.minutes, isCurrent = { true }, resolve = { throw CancellationException("newer reload") }) {}
			}
		}

		entry.startPosition shouldBe null
	}
})
