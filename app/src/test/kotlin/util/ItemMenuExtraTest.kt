package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.util.UUID

class ItemMenuExtraTest : FunSpec({
	fun item(type: BaseItemKind, positionTicks: Long?, seriesId: UUID? = null): BaseItemDto {
		val id = UUID.randomUUID()
		val userData = positionTicks?.let {
			UserItemDataDto(playbackPositionTicks = it, playCount = 0, isFavorite = false, played = false, key = "key", itemId = id)
		}
		return BaseItemDto(id = id, type = type, userData = userData, seriesId = seriesId)
	}

	test("a half-watched film offers to reset its progress") {
		ItemMenuExtra.forItem(item(BaseItemKind.MOVIE, positionTicks = 42)) shouldBe listOf(ItemMenuExtra.RESET_PROGRESS)
	}

	test("an episode offers its series, and the reset too when half-watched") {
		val series = UUID.randomUUID()
		ItemMenuExtra.forItem(item(BaseItemKind.EPISODE, positionTicks = 0, seriesId = series)) shouldBe listOf(ItemMenuExtra.GO_TO_SERIES)
		ItemMenuExtra.forItem(item(BaseItemKind.EPISODE, positionTicks = 42, seriesId = series)) shouldBe
			listOf(ItemMenuExtra.RESET_PROGRESS, ItemMenuExtra.GO_TO_SERIES)
	}

	test("nothing is added without progress or a series") {
		ItemMenuExtra.forItem(item(BaseItemKind.MOVIE, positionTicks = 0)).shouldBeEmpty()
		ItemMenuExtra.forItem(item(BaseItemKind.MOVIE, positionTicks = null)).shouldBeEmpty()
		ItemMenuExtra.forItem(item(BaseItemKind.EPISODE, positionTicks = null, seriesId = null)).shouldBeEmpty()
	}

	test("menu ids stay clear of upstream's and map back to their entry") {
		for (extra in ItemMenuExtra.entries) {
			(extra.menuId > KeyProcessor.MENU_TOGGLE_SHUFFLE) shouldBe true
			ItemMenuExtra.forMenuId(extra.menuId) shouldBe extra
		}
		ItemMenuExtra.forMenuId(KeyProcessor.MENU_PLAY) shouldBe null
	}
})
