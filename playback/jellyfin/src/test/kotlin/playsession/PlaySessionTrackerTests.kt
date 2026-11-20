package org.jellyfin.playback.jellyfin.playsession

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.core.model.PlayState
import kotlin.time.Duration.Companion.seconds

class PlaySessionTrackerTests : FunSpec({
	test("nothing is reported before playback starts") {
		val tracker = PlaySessionTracker<String>()

		tracker.update(null, PlayState.PAUSED, null).shouldBeEmpty()
		tracker.update("film", PlayState.PAUSED, 0.seconds).shouldBeEmpty()
	}

	test("playing starts the session once, then pauses and ticks report progress") {
		val tracker = PlaySessionTracker<String>()

		tracker.update("film", PlayState.PLAYING, 0.seconds) shouldContainExactly listOf(PlaySessionReport.Start("film"))
		tracker.update("film", PlayState.PAUSED, 10.seconds) shouldContainExactly listOf(PlaySessionReport.Progress("film"))
		tracker.update("film", PlayState.PLAYING, 10.seconds) shouldContainExactly listOf(PlaySessionReport.Progress("film"))
	}

	test("stopping reports the started session even after the queue was cleared") {
		val tracker = PlaySessionTracker<String>()
		tracker.update("film", PlayState.PLAYING, 0.seconds)
		tracker.update("film", PlayState.PLAYING, 42.seconds)

		tracker.update(null, PlayState.STOPPED, null) shouldContainExactly listOf(
			PlaySessionReport.Stop("film", 42.seconds, failed = false),
		)
		tracker.active shouldBe null
	}

	test("a stop after the queue was cleared reports the last observed position") {
		val tracker = PlaySessionTracker<String>()
		tracker.update("film", PlayState.PLAYING, 0.seconds)
		tracker.observe("film", 61.seconds)
		tracker.observe("other", 5.seconds)

		tracker.update(null, PlayState.STOPPED, null) shouldContainExactly listOf(
			PlaySessionReport.Stop("film", 61.seconds, failed = false),
		)
	}

	test("moving to the next entry while playing stops the previous one and starts the next") {
		val tracker = PlaySessionTracker<String>()
		tracker.update("episode 1", PlayState.PLAYING, 0.seconds)
		tracker.update("episode 1", PlayState.PLAYING, 1300.seconds)

		tracker.update("episode 2", PlayState.PLAYING, 0.seconds) shouldContainExactly listOf(
			PlaySessionReport.Stop("episode 1", 1300.seconds, failed = false),
			PlaySessionReport.Start("episode 2"),
		)
	}

	test("the next entry starts only once its stream is known") {
		val tracker = PlaySessionTracker<String>()
		tracker.update("episode 1", PlayState.PLAYING, 0.seconds)

		tracker.update(null, PlayState.PLAYING, 0.seconds) shouldContainExactly listOf(
			PlaySessionReport.Stop("episode 1", 0.seconds, failed = false),
		)
		tracker.update("episode 2", PlayState.PLAYING, 0.seconds) shouldContainExactly listOf(PlaySessionReport.Start("episode 2"))
	}

	test("an error stops the session as failed, and the stop that follows reports nothing more") {
		val tracker = PlaySessionTracker<String>()
		tracker.update("film", PlayState.PLAYING, 5.seconds)

		tracker.update("film", PlayState.ERROR, 7.seconds) shouldContainExactly listOf(
			PlaySessionReport.Stop("film", 7.seconds, failed = true),
		)
		tracker.update("film", PlayState.STOPPED, 7.seconds).shouldBeEmpty()
	}
})
