package org.jellyfin.androidtv.ui.presentation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import java.util.UUID

class LibraryCardBackdropTest : FunSpec({
	fun item(type: BaseItemKind, backdrops: List<String>? = null) = BaseItemDto(
		id = UUID.randomUUID(),
		type = type,
		backdropImageTags = backdrops,
	)

	test("only libraries get a title's backdrop") {
		isLibraryCard(item(BaseItemKind.USER_VIEW)) shouldBe true
		isLibraryCard(item(BaseItemKind.COLLECTION_FOLDER)) shouldBe true
		isLibraryCard(item(BaseItemKind.FOLDER)) shouldBe false
		isLibraryCard(item(BaseItemKind.MOVIE)) shouldBe false
		isLibraryCard(null) shouldBe false
	}

	test("the card takes the first backdrop of the first title that has one") {
		val bare = item(BaseItemKind.MUSIC_ALBUM)
		val film = item(BaseItemKind.MOVIE, backdrops = listOf("first", "second"))

		val backdrop = libraryCardBackdrop(listOf(bare, film))

		backdrop?.item shouldBe film.id
		backdrop?.type shouldBe ImageType.BACKDROP
		backdrop?.tag shouldBe "first"
		backdrop?.index shouldBe 0
	}

	test("a library without backdrops has none") {
		libraryCardBackdrop(listOf(item(BaseItemKind.MUSIC_ALBUM, backdrops = emptyList()))) shouldBe null
		libraryCardBackdrop(emptyList()) shouldBe null
	}
})
