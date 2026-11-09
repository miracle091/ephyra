package org.jellyfin.androidtv.ui.itemdetail

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.util.UUID

class DetailsOverviewTextTest : FunSpec({
	val id = UUID.randomUUID()

	fun episode(position: Long, runtime: Long?, percentage: Double? = null) = BaseItemDto(
		id = id,
		type = BaseItemKind.EPISODE,
		runTimeTicks = runtime,
		userData = UserItemDataDto(
			playedPercentage = percentage,
			playbackPositionTicks = position,
			playCount = 0,
			isFavorite = false,
			played = false,
			key = "",
			itemId = id,
		),
	)

	test("the tagline is the first non-blank one") {
		DetailsTagline.of(listOf(" ", "  Hello  ", "Other")) shouldBe "Hello"
	}

	test("no tagline hides the line") {
		DetailsTagline.of(null) shouldBe null
		DetailsTagline.of(emptyList()) shouldBe null
	}

	test("the next up title joins the episode label and the name") {
		DetailsNextUp.title("S1:E3", "Pilot") shouldBe "S1:E3 · Pilot"
		DetailsNextUp.title("S1:E3", null) shouldBe "S1:E3"
		DetailsNextUp.title("", "Pilot") shouldBe "Pilot"
	}

	test("an unstarted episode has no progress") {
		DetailsNextUp.progress(episode(position = 0, runtime = 1000)) shouldBe null
	}

	test("a half-watched episode has its fraction") {
		DetailsNextUp.progress(episode(position = 250, runtime = 1000)) shouldBe 0.25f
		DetailsNextUp.progress(episode(position = 0, runtime = 1000, percentage = 50.0)) shouldBe 0.5f
	}

	test("a finished episode has no progress") {
		DetailsNextUp.progress(episode(position = 1000, runtime = 1000)) shouldBe null
	}
})
