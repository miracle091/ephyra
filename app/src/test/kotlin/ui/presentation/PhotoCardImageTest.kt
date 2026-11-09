package org.jellyfin.androidtv.ui.presentation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ImageType
import java.util.UUID

class PhotoCardImageTest : FunSpec({
	fun photo(tag: String? = null) = BaseItemDto(
		id = UUID.randomUUID(),
		type = BaseItemKind.PHOTO,
		imageTags = tag?.let { mapOf(ImageType.PRIMARY to it) },
	)

	test("a photo library card is told apart from the others") {
		val photos = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.COLLECTION_FOLDER, collectionType = CollectionType.PHOTOS)
		val movies = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.COLLECTION_FOLDER, collectionType = CollectionType.MOVIES)

		isPhotoLibraryCard(photos) shouldBe true
		isPhotoLibraryCard(movies) shouldBe false
	}

	test("the card takes the first photo that has a picture") {
		val bare = photo()
		val shot = photo(tag = "p")

		val image = photoCardImage(listOf(bare, shot))

		image?.item shouldBe shot.id
		image?.type shouldBe ImageType.PRIMARY
	}

	test("no photo with a picture gives none") {
		photoCardImage(listOf(photo())) shouldBe null
		photoCardImage(emptyList()) shouldBe null
	}
})
