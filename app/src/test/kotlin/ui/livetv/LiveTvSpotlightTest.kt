package org.jellyfin.androidtv.ui.livetv

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LiveTvSpotlightTest : FunSpec({
	val now = LocalDateTime.of(2026, 11, 27, 23, 20)
	fun program(end: LocalDateTime?) = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.TV_PROGRAM, endDate = end)

	test("the spotlight loads again a few seconds after the first of its programmes ends") {
		onAirReloadDelay(listOf(program(now.plusMinutes(40)), program(now.plusMinutes(10))), now) shouldBe 10.minutes + 5.seconds
	}

	test("it waits at least a minute, also with a programme already over or none at all") {
		onAirReloadDelay(listOf(program(now.minusMinutes(2))), now) shouldBe 1.minutes
		onAirReloadDelay(listOf(program(null)), now) shouldBe 1.minutes
		onAirReloadDelay(emptyList(), now) shouldBe 1.minutes
	}
})
