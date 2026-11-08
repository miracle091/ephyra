package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.time.LocalDateTime
import java.util.UUID

class NextProgramTest : FunSpec({
	val end = LocalDateTime.of(2026, 11, 8, 22, 45)

	fun program(name: String?, start: LocalDateTime?) =
		BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.PROGRAM, name = name, startDate = start)

	test("The next programme is the earliest one starting at or after the end of the programme on air") {
		val later = program("Late film", end.plusHours(1))
		val next = program("Sports news", end)

		NextProgram.pick(listOf(later, next), end) shouldBe next
	}

	test("A programme that started before the end of the one on air is not the next one") {
		val onAir = program("Match", end.minusHours(2))

		NextProgram.pick(listOf(onAir), end) shouldBe null
	}

	test("A programme without a title or a start time is skipped") {
		val untitled = program(" ", end)
		val undated = program("Undated", null)
		val valid = program("Weather", end.plusMinutes(30))

		NextProgram.pick(listOf(untitled, undated, valid), end) shouldBe valid
	}

	test("There is no next programme without a list or without the end of the current one") {
		NextProgram.pick(null, end) shouldBe null
		NextProgram.pick(listOf(program("Weather", end)), null) shouldBe null
		NextProgram.pick(emptyList(), end) shouldBe null
	}
})
