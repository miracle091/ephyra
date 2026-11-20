package org.jellyfin.androidtv.ui.player.video

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.MediaSegmentType
import org.jellyfin.sdk.model.extensions.inWholeTicks
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AskToSkipSegmentTest : FunSpec({
	fun segment(type: MediaSegmentType, start: Duration, end: Duration) = MediaSegmentDto(
		id = UUID.randomUUID(),
		itemId = UUID.randomUUID(),
		type = type,
		startTicks = start.inWholeTicks,
		endTicks = end.inWholeTicks,
	)

	val intro = segment(MediaSegmentType.INTRO, 30.seconds, 2.minutes)
	val recap = segment(MediaSegmentType.RECAP, 0.seconds, 30.seconds)
	val outro = segment(MediaSegmentType.OUTRO, 40.minutes, 42.minutes)
	val segments = listOf(recap, intro, outro)
	val actions = mapOf(
		MediaSegmentType.INTRO to MediaSegmentAction.ASK_TO_SKIP,
		MediaSegmentType.OUTRO to MediaSegmentAction.ASK_TO_SKIP,
		MediaSegmentType.RECAP to MediaSegmentAction.NOTHING,
	)
	val actionOf = { segment: MediaSegmentDto -> actions.getValue(segment.type) }

	test("inside an ask-to-skip segment the button jumps to its end") {
		askToSkipTarget(segments, actionOf, 30.seconds) shouldBe 2.minutes
		askToSkipTarget(segments, actionOf, 90.seconds) shouldBe 2.minutes
		askToSkipTarget(segments, actionOf, 41.minutes) shouldBe 42.minutes
	}

	test("segments set to do nothing, the end of a segment and the time between show no button") {
		askToSkipTarget(segments, actionOf, 10.seconds) shouldBe null
		askToSkipTarget(segments, actionOf, 2.minutes) shouldBe null
		askToSkipTarget(segments, actionOf, 20.minutes) shouldBe null
		askToSkipTarget(emptyList(), actionOf, 1.minutes) shouldBe null
	}
})
