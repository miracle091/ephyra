package org.jellyfin.androidtv.ui.browsing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ItemFields
import java.util.UUID

class BrowseGridItemsRequestTest : FunSpec({
	val library = BaseItemDto(
		id = UUID.randomUUID(),
		type = BaseItemKind.COLLECTION_FOLDER,
		collectionType = CollectionType.MOVIES,
	)

	test("grid pages leave out the heavy fields the grid never shows") {
		val fields = createBrowseGridItemsRequest(library).fields.orEmpty()

		fields shouldNotContainAnyOf listOf(
			ItemFields.CHAPTERS,
			ItemFields.TRICKPLAY,
			ItemFields.MEDIA_STREAMS,
			ItemFields.PATH,
			ItemFields.OVERVIEW,
			ItemFields.TAGLINES,
		)
	}

	test("grid pages keep what the cards and the info row show") {
		val fields = createBrowseGridItemsRequest(library).fields.orEmpty()

		fields shouldContainAll listOf(
			ItemFields.MEDIA_SOURCES,
			ItemFields.CHILD_COUNT,
			ItemFields.ITEM_COUNTS,
			ItemFields.CUMULATIVE_RUN_TIME_TICKS,
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.DISPLAY_PREFERENCES_ID,
		)
	}
})
