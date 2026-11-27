package org.jellyfin.androidtv.ui.livetv

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.time.LocalDateTime
import java.util.UUID

class NoProgramDataTest : FunSpec({
	val now = LocalDateTime.of(2026, 11, 27, 23, 0)

	test("a filler cell is not a programme, so the guide never marks it on air") {
		val context = mockk<Context> { every { getString(any()) } returns "No program data" }
		isNoProgramData(createNoProgramDataBaseItem(context, UUID.randomUUID(), now, now.plusMinutes(30))) shouldBe true
	}

	test("a real programme is not a filler") {
		isNoProgramData(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.TV_PROGRAM, startDate = now)) shouldBe false
	}
})
