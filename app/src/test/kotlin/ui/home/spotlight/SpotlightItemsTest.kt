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
			R.string.velella_runtime_hours_minutes -> "%d h %d min".format(*args)
			R.string.velella_runtime_minutes -> "%d min".format(*args)
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

	test("the info row lists up to three genres joined by a dot") {
		spotlightGenres(item(BaseItemKind.MOVIE, genres = listOf("Thriller", " ", "Drama", "Crime", "Mystery"))) shouldBe
			"Thriller · Drama · Crime"
		spotlightGenres(item(BaseItemKind.MOVIE, genres = listOf("Horror"))) shouldBe "Horror"
		spotlightGenres(item(BaseItemKind.MOVIE)) shouldBe null
		spotlightGenres(item(BaseItemKind.MOVIE, genres = listOf(""))) shouldBe null
	}

	test("the overview comes first, the tagline only when there is no overview") {
		val film = item(BaseItemKind.MOVIE).copy(overview = " A count moves to London. ", taglines = listOf("The strangest passion"))
		spotlightOverview(film) shouldBe "A count moves to London."
		spotlightOverview(film.copy(overview = " ")) shouldBe "The strangest passion"
		spotlightOverview(film.copy(overview = null, taglines = listOf(""))) shouldBe null
	}

	test("films show their running time; series leave it out") {
		spotlightRuntime(item(BaseItemKind.MOVIE, minutes = 166), getString) shouldBe "2 h 46 min"
		spotlightRuntime(item(BaseItemKind.MOVIE, minutes = 45), getString) shouldBe "45 min"
		spotlightRuntime(item(BaseItemKind.SERIES, minutes = 45), getString) shouldBe null
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
