package org.jellyfin.playback.jellyfin.mediasegment

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.core.timedevent.BlockActivation
import org.jellyfin.playback.core.timedevent.TimedEvent
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.MediaSegmentType
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val TICKS_PER_SECOND = 10_000_000L

private fun segment(type: MediaSegmentType) = MediaSegmentDto(
	id = UUID.randomUUID(),
	itemId = UUID.randomUUID(),
	type = type,
	startTicks = 30 * TICKS_PER_SECOND,
	endTicks = 90 * TICKS_PER_SECOND,
)

class SkipEventsTests : FunSpec({
	test("a segment the user doesn't skip gets no event") {
		segment(MediaSegmentType.INTRO).skipEvents(shouldSkip = { false }) {}.shouldBeEmpty()
	}

	test("a segment the user skips jumps to its end when playback reaches it") {
		val skips = mutableListOf<Duration>()
		val event = segment(MediaSegmentType.OUTRO).skipEvents(shouldSkip = { it.type == MediaSegmentType.OUTRO }) { skips += it }
			.single() as TimedEvent.Block

		event.start shouldBe 30.seconds
		event.end shouldBe 90.seconds
		event.onActivate?.invoke(BlockActivation.Natural(30.seconds))

		skips shouldBe listOf(90.seconds)
	}

	test("seeking into a skipped segment stays there") {
		val skips = mutableListOf<Duration>()
		val event = segment(MediaSegmentType.INTRO).skipEvents(shouldSkip = { true }) { skips += it }.single() as TimedEvent.Block

		event.onActivate?.invoke(BlockActivation.Seek(from = 0.seconds, to = 40.seconds))

		skips.shouldBeEmpty()
	}
})
