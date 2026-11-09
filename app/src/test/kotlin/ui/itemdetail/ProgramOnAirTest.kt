package org.jellyfin.androidtv.ui.itemdetail

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.time.LocalDateTime
import java.util.UUID

class ProgramOnAirTest : FunSpec({
	val now = LocalDateTime.of(2026, 11, 9, 21, 0)

	fun program(end: LocalDateTime?) = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.PROGRAM, endDate = end)

	test("a programme that ended before now is over") {
		hasProgramEnded(program(now.minusMinutes(1)), now) shouldBe true
	}

	test("a programme still on air is not over") {
		hasProgramEnded(program(now.plusMinutes(30)), now) shouldBe false
	}

	test("a programme without an end time or no programme at all is not over") {
		hasProgramEnded(program(null), now) shouldBe false
		hasProgramEnded(null, now) shouldBe false
	}
})
