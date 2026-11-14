package org.jellyfin.androidtv.ui.presentation

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class CardDisplayRulesTest : FunSpec({
	val wide = ImageHelper.ASPECT_RATIO_16_9.toFloat()
	val poster = ImageHelper.ASPECT_RATIO_2_3.toFloat()
	val tolerance = 0.0001f

	fun item(type: BaseItemKind, primaryImageAspectRatio: Double? = null, parentThumbItemId: UUID? = null) = BaseItemDto(
		id = UUID.randomUUID(),
		type = type,
		primaryImageAspectRatio = primaryImageAspectRatio,
		parentThumbItemId = parentThumbItemId,
	)

	context("imageTypeAspectRatio") {
		test("banner and thumb have fixed ratios") {
			imageTypeAspectRatio(ImageType.BANNER, 0.5f) shouldBe ImageHelper.ASPECT_RATIO_BANNER.toFloat()
			imageTypeAspectRatio(ImageType.THUMB, 0.5f) shouldBe wide
		}

		test("poster uses the fallback") {
			imageTypeAspectRatio(ImageType.POSTER, 0.5f) shouldBe 0.5f
		}
	}

	context("baseItemDefaultAspectRatio") {
		test("an item preferring its parent thumb is wide when it has one") {
			val withThumb = item(BaseItemKind.EPISODE, parentThumbItemId = UUID.randomUUID())
			baseItemDefaultAspectRatio(withThumb, preferParentThumb = true) shouldBe wide
		}

		test("an episode uses its primary image ratio first") {
			baseItemDefaultAspectRatio(item(BaseItemKind.EPISODE, primaryImageAspectRatio = 1.5), preferParentThumb = false) shouldBe 1.5f
		}

		test("an episode without a primary ratio but with a parent thumb is wide") {
			val episode = item(BaseItemKind.EPISODE, parentThumbItemId = UUID.randomUUID())
			baseItemDefaultAspectRatio(episode, preferParentThumb = false) shouldBe wide
		}

		test("a library is wide") {
			baseItemDefaultAspectRatio(item(BaseItemKind.USER_VIEW), preferParentThumb = false) shouldBe wide
		}

		test("anything else uses the primary ratio, or 7:9 when there is none") {
			baseItemDefaultAspectRatio(item(BaseItemKind.MOVIE, primaryImageAspectRatio = 0.66), preferParentThumb = false)
				.shouldBe(0.66f.plusOrMinus(tolerance))
			baseItemDefaultAspectRatio(item(BaseItemKind.MOVIE), preferParentThumb = false) shouldBe ImageHelper.ASPECT_RATIO_7_9.toFloat()
			baseItemDefaultAspectRatio(null, preferParentThumb = false) shouldBe ImageHelper.ASPECT_RATIO_7_9.toFloat()
		}
	}

	context("itemKindIcon") {
		test("each group of kinds has its icon") {
			itemKindIcon(BaseItemKind.AUDIO) shouldBe R.drawable.ic_music_album
			itemKindIcon(BaseItemKind.MUSIC_ARTIST) shouldBe R.drawable.ic_user
			itemKindIcon(BaseItemKind.EPISODE) shouldBe R.drawable.ic_tv
			itemKindIcon(BaseItemKind.PLAYLIST) shouldBe R.drawable.ic_folder
			itemKindIcon(BaseItemKind.PHOTO) shouldBe R.drawable.ic_photo
			itemKindIcon(BaseItemKind.MOVIE) shouldBe R.drawable.ic_clapperboard
			itemKindIcon(null) shouldBe R.drawable.ic_clapperboard
		}
	}

	context("itemKindAspectRatio") {
		test("music and people are square when narrow or uniform") {
			itemKindAspectRatio(BaseItemKind.MUSIC_ALBUM, 0.7f, ImageType.POSTER, uniformAspect = false, preferSeriesPoster = false) shouldBe 1f
			itemKindAspectRatio(BaseItemKind.PERSON, 1.2f, ImageType.POSTER, uniformAspect = true, preferSeriesPoster = false) shouldBe 1f
			itemKindAspectRatio(BaseItemKind.PERSON, 1.2f, ImageType.POSTER, uniformAspect = false, preferSeriesPoster = false) shouldBe 1.2f
		}

		test("series and movies are 2:3 posters for the poster image type only") {
			itemKindAspectRatio(BaseItemKind.SERIES, 1.7f, ImageType.POSTER, uniformAspect = false, preferSeriesPoster = false) shouldBe poster
			itemKindAspectRatio(BaseItemKind.MOVIE, 1.7f, ImageType.THUMB, uniformAspect = false, preferSeriesPoster = false) shouldBe 1.7f
		}

		test("an episode is a poster when it prefers the series poster, else wide") {
			itemKindAspectRatio(BaseItemKind.EPISODE, 1f, ImageType.POSTER, uniformAspect = false, preferSeriesPoster = true) shouldBe poster
			itemKindAspectRatio(BaseItemKind.EPISODE, 1f, ImageType.POSTER, uniformAspect = false, preferSeriesPoster = false) shouldBe wide
		}

		test("libraries are wide and folders keep the base ratio") {
			itemKindAspectRatio(BaseItemKind.USER_VIEW, 1f, ImageType.POSTER, uniformAspect = false, preferSeriesPoster = false) shouldBe wide
			itemKindAspectRatio(BaseItemKind.FOLDER, 0.9f, ImageType.POSTER, uniformAspect = false, preferSeriesPoster = false) shouldBe 0.9f
		}
	}

	context("resolveAspectRatio") {
		test("the config ratio wins, then the image's, then square") {
			resolveAspectRatio(1.5f, 0.5f) shouldBe 1.5f
			resolveAspectRatio(0.05f, 0.5f) shouldBe 0.5f
			resolveAspectRatio(0.05f, 0.01f) shouldBe 1f
			resolveAspectRatio(0.05f, null) shouldBe 1f
		}
	}

	context("cardSize") {
		test("a static height item uses the presenter's static height") {
			cardSize(0.5f, itemStaticHeight = true, staticHeight = 200, cardHeight = null) shouldBe DpSize(100.dp, 200.dp)
		}

		test("a fixed card height applies, shrunk to 130:150 for landscape cards") {
			cardSize(0.5f, itemStaticHeight = false, staticHeight = 200, cardHeight = 300) shouldBe DpSize(150.dp, 300.dp)
			val landscape = cardSize(2f, itemStaticHeight = false, staticHeight = 200, cardHeight = 150)
			landscape.height shouldBe 150.dp * (130f / 150f)
		}

		test("without any height a portrait card is 150dp and a landscape one 130dp") {
			cardSize(0.5f, itemStaticHeight = false, staticHeight = 200, cardHeight = null) shouldBe DpSize(75.dp, 150.dp)
			cardSize(2f, itemStaticHeight = false, staticHeight = 200, cardHeight = null) shouldBe DpSize(260.dp, 130.dp)
		}
	}
})
