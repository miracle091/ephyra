package org.jellyfin.androidtv.ui.home

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.itemhandling.GridButtonBaseRowItem
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class SeeAllButtonTest : FunSpec({
	val library = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.COLLECTION_FOLDER)
	val card = GridButtonBaseRowItem(GridButton(1, "Film"))
	val seeAll = GridButtonBaseRowItem(SeeAllButton(library, "See all", card))

	test("a loaded row gets its See all card at the end") {
		needsSeeAllCard(listOf(card)) shouldBe true
	}

	test("an empty row, or one already ending with See all, gets none") {
		needsSeeAllCard(emptyList()) shouldBe false
		needsSeeAllCard(listOf(card, seeAll)) shouldBe false
	}
})
