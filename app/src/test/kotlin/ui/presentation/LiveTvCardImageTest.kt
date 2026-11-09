package org.jellyfin.androidtv.ui.presentation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import java.util.UUID

class LiveTvCardImageTest : FunSpec({
	fun channel(logo: String? = null) = BaseItemDto(
		id = UUID.randomUUID(),
		type = BaseItemKind.TV_CHANNEL,
		imageTags = logo?.let { mapOf(ImageType.PRIMARY to it) },
	)

	test("the card takes a channel's logo, its primary image") {
		val withLogo = channel(logo = "logo")

		val image = liveTvCardLogo(listOf(withLogo), pick = 0)

		image?.item shouldBe withLogo.id
		image?.type shouldBe ImageType.PRIMARY
	}

	test("the pick chooses among the channels that have a logo") {
		val bare = channel()
		val first = channel(logo = "1")
		val second = channel(logo = "2")

		liveTvCardLogo(listOf(bare, first, second), pick = 0)?.item shouldBe first.id
		liveTvCardLogo(listOf(bare, first, second), pick = 1)?.item shouldBe second.id
		liveTvCardLogo(listOf(bare, first, second), pick = 2)?.item shouldBe first.id
	}

	test("no channel with a logo gives none") {
		liveTvCardLogo(listOf(channel()), pick = 0) shouldBe null
		liveTvCardLogo(emptyList(), pick = 3) shouldBe null
	}
})
