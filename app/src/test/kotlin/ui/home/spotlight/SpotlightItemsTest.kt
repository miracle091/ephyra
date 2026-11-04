package org.jellyfin.androidtv.ui.home.spotlight

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class SpotlightItemsTest : FunSpec({
	val getString: (Int, Array<out Any>) -> String = { res, args ->
		when (res) {
			R.string.ephyra_runtime_hours_minutes -> "%d h %d min".format(*args)
			R.string.ephyra_runtime_minutes -> "%d min".format(*args)
			else -> error("unexpected string $res")
		}
	}

	fun item(type: BaseItemKind, minutes: Long? = null, genres: List<String>? = null) = BaseItemDto(
		id = UUID.randomUUID(),
		type = type,
		productionYear = 2024,
		runTimeTicks = minutes?.let { it * 600_000_000L },
		officialRating = "PG-13",
		genres = genres,
	)

	test("the overline names the first genre after the label") {
		spotlightOverline("Spotlight", item(BaseItemKind.MOVIE, genres = listOf("Thriller", "Drama"))) shouldBe "Spotlight · Thriller"
		spotlightOverline("Spotlight", item(BaseItemKind.MOVIE)) shouldBe "Spotlight"
	}

	test("films show year, running time and rating; series leave the running time out") {
		spotlightMeta(item(BaseItemKind.MOVIE, minutes = 166), getString) shouldBe "2024 · 2 h 46 min · PG-13"
		spotlightMeta(item(BaseItemKind.MOVIE, minutes = 45), getString) shouldBe "2024 · 45 min · PG-13"
		spotlightMeta(item(BaseItemKind.SERIES, minutes = 45), getString) shouldBe "2024 · PG-13"
	}

	test("play starts a series from its next up episode, else from the series itself") {
		val series = item(BaseItemKind.SERIES)
		val episode = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE, seriesId = series.id)
		spotlightPlayTarget(series, episode) shouldBe episode.id
		spotlightPlayTarget(series, null) shouldBe series.id
		// An episode of another series is ignored
		spotlightPlayTarget(series, episode.copy(seriesId = UUID.randomUUID())) shouldBe series.id
		val movie = item(BaseItemKind.MOVIE)
		spotlightPlayTarget(movie, null) shouldBe movie.id
	}
})
