package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class OsdHeaderTest : FunSpec({
	test("An episode's overline is the series and the episode number") {
		val item = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE, seriesName = "The Seasons")

		OsdHeader.overlineOf(item, "S2:E4") shouldBe "The Seasons · S2:E4"
	}

	test("An episode without a number keeps the series alone") {
		val item = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE, seriesName = "The Seasons")

		OsdHeader.overlineOf(item, "") shouldBe "The Seasons"
	}

	test("A film's overline is its genres") {
		val item = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE, genres = listOf("Drama", "TV Movie"))

		OsdHeader.overlineOf(item, "") shouldBe "Drama · TV Movie"
	}

	test("A film without genres has no overline") {
		val item = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE)

		OsdHeader.overlineOf(item, "") shouldBe ""
	}

	test("A live channel's overline is Live, its number and its name") {
		val item = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.TV_CHANNEL, number = "102", name = "Sport Uno")

		OsdHeader.overlineOf(item, "", "Live") shouldBe "Live · 102 · Sport Uno"
	}

	test("A live channel's title is the programme on air, else the channel's name") {
		val program = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.PROGRAM, name = "Match of the Day")
		val onAir = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.TV_CHANNEL, name = "Sport Uno", currentProgram = program)
		val idle = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.TV_CHANNEL, name = "Sport Uno")

		OsdHeader.titleOf(onAir) shouldBe "Match of the Day"
		OsdHeader.titleOf(idle) shouldBe "Sport Uno"
	}
})
