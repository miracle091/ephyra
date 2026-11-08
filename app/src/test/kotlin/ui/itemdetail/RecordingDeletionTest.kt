package org.jellyfin.androidtv.ui.itemdetail

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class RecordingDeletionTest : FunSpec({
	val recordingsFolder = UUID.randomUUID()
	val moviesLibrary = UUID.randomUUID()

	fun item(type: BaseItemKind, canDelete: Boolean? = true) = BaseItemDto(id = UUID.randomUUID(), type = type, canDelete = canDelete)

	test("a recorded film in a recording folder can be deleted") {
		canDeleteFromTv(item(BaseItemKind.MOVIE), listOf(moviesLibrary, recordingsFolder), listOf(recordingsFolder)) shouldBe true
	}

	test("a recording the server marks as such can be deleted") {
		canDeleteFromTv(item(BaseItemKind.RECORDING), emptyList(), emptyList()) shouldBe true
	}

	test("a film of the library cannot be deleted, even when the server would allow it") {
		canDeleteFromTv(item(BaseItemKind.MOVIE), listOf(moviesLibrary), listOf(recordingsFolder)) shouldBe false
	}

	test("an episode of the library cannot be deleted") {
		canDeleteFromTv(item(BaseItemKind.EPISODE), listOf(moviesLibrary), listOf(recordingsFolder)) shouldBe false
	}

	test("a recording the server does not allow to delete stays") {
		canDeleteFromTv(item(BaseItemKind.MOVIE, canDelete = false), listOf(recordingsFolder), listOf(recordingsFolder)) shouldBe false
		canDeleteFromTv(item(BaseItemKind.RECORDING, canDelete = null), emptyList(), emptyList()) shouldBe false
	}
})
