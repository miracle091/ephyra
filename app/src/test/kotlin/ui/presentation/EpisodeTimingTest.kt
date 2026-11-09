package org.jellyfin.androidtv.ui.presentation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalTime
import java.util.Locale

private const val TICKS_PER_MINUTE = 600_000_000L

class EpisodeTimingTest : FunSpec({
	test("an unwatched episode ends after its whole runtime") {
		endsAt(47 * TICKS_PER_MINUTE, 0, LocalTime.of(22, 0)) shouldBe LocalTime.of(22, 47)
		endsAt(47 * TICKS_PER_MINUTE, null, LocalTime.of(22, 0)) shouldBe LocalTime.of(22, 47)
	}

	test("a partly watched episode only counts what is left") {
		endsAt(60 * TICKS_PER_MINUTE, 45 * TICKS_PER_MINUTE, LocalTime.of(20, 0)) shouldBe LocalTime.of(20, 15)
	}

	test("a position at or past the end counts the whole runtime") {
		endsAt(30 * TICKS_PER_MINUTE, 30 * TICKS_PER_MINUTE, LocalTime.of(20, 0)) shouldBe LocalTime.of(20, 30)
		endsAt(30 * TICKS_PER_MINUTE, 40 * TICKS_PER_MINUTE, LocalTime.of(20, 0)) shouldBe LocalTime.of(20, 30)
	}

	test("the end time wraps past midnight") {
		endsAt(30 * TICKS_PER_MINUTE, 0, LocalTime.of(23, 50)) shouldBe LocalTime.of(0, 20)
	}

	test("an unknown runtime has no end time") {
		endsAt(null, 0, LocalTime.NOON) shouldBe null
		endsAt(0, 0, LocalTime.NOON) shouldBe null
	}

	test("the runtime rounds to whole minutes, at least one") {
		runtimeMinutes(47 * TICKS_PER_MINUTE + TICKS_PER_MINUTE / 3) shouldBe 47
		runtimeMinutes(TICKS_PER_MINUTE / 10) shouldBe 1
		runtimeMinutes(null) shouldBe null
	}

	test("the meta line joins the known parts") {
		episodeMetaLine("47m", 8.24f, "Ends at 22:45", Locale.US) shouldBe "47m · ★ 8.2 · Ends at 22:45"
		episodeMetaLine("47m", null, "Ends at 22:45", Locale.US) shouldBe "47m · Ends at 22:45"
		episodeMetaLine(null, 7f, null, Locale.US) shouldBe "★ 7.0"
	}

	test("the compact title leads with the episode number") {
		compactEpisodeTitle(1, null, "Pilot") shouldBe "1. Pilot"
		compactEpisodeTitle(1, 2, "Pilot") shouldBe "1-2. Pilot"
		compactEpisodeTitle(null, null, "Pilot") shouldBe "Pilot"
		compactEpisodeTitle(3, null, null) shouldBe "3"
	}

	test("the meta line is null when nothing is known") {
		episodeMetaLine(null, null, null, Locale.US) shouldBe null
		episodeMetaLine(null, 0f, null, Locale.US) shouldBe null
	}
})
