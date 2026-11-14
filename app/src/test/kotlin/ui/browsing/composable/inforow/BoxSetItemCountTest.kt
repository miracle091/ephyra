package org.jellyfin.androidtv.ui.browsing.composable.inforow

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class BoxSetItemCountTest : FunSpec({
	fun boxSet(movieCount: Int? = null, seriesCount: Int? = null, childCount: Int? = null) = BaseItemDto(
		id = UUID.randomUUID(),
		type = BaseItemKind.BOX_SET,
		movieCount = movieCount,
		seriesCount = seriesCount,
		childCount = childCount,
	)

	test("the movie count comes first, then the series count, then the children") {
		boxSetItemCount(boxSet(movieCount = 3, seriesCount = 2, childCount = 5)) shouldBe 3
		boxSetItemCount(boxSet(seriesCount = 2, childCount = 5)) shouldBe 2
		boxSetItemCount(boxSet(childCount = 5)) shouldBe 5
	}

	test("a box set without any count has none to show") {
		boxSetItemCount(boxSet()) shouldBe null
	}
})
