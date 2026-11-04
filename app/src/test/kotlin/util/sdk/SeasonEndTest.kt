package org.jellyfin.androidtv.util.sdk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class SeasonEndTest : FunSpec({
	val season1 = UUID.randomUUID()
	val season2 = UUID.randomUUID()
	val specials = UUID.randomUUID()
	fun episode(seasonId: UUID?) = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE, seasonId = seasonId)

	test("only the episodes of the started one's season are queued, specials in between left out") {
		val e1 = episode(season1)
		val e2 = episode(season1)
		val special = episode(specials)
		val e3 = episode(season1)
		val next = episode(season2)

		episodesOfSeason(listOf(e1, e2, special, e3, next), e1) shouldBe listOf(e1, e2, e3)
	}

	test("without a season id the queue is left as it is") {
		val e1 = episode(null)
		val e2 = episode(season2)
		episodesOfSeason(listOf(e1, e2), e1) shouldBe listOf(e1, e2)
	}
})
