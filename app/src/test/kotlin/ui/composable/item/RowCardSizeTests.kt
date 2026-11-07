package org.jellyfin.androidtv.ui.composable.item

import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class RowCardSizeTests : FunSpec({
	// A 1080p TV at 320 dpi, as the Shield reports it
	val screenWidth = 960.dp

	test("the cards of a row fill it edge to edge, gaps included") {
		listOf(
			ROW_CARDS_WIDE to WIDE_ASPECT_RATIO,
			ROW_CARDS_POSTER to POSTER_ASPECT_RATIO,
			ROW_CARDS_WIDE_SMALL to WIDE_ASPECT_RATIO,
		).forEach { (count, aspectRatio) ->
			val width = rowCardHeight(screenWidth, count, aspectRatio).value * aspectRatio
			// 32dp margins on both sides, 8dp between cards
			(width * count + 8f * (count - 1)) shouldBe (896f plusOrMinus 0.5f)
		}
	}

	test("four wide cards are a little shorter than upstream's 126dp") {
		rowCardHeight(screenWidth, ROW_CARDS_WIDE, WIDE_ASPECT_RATIO).value shouldBe (122.6f plusOrMinus 0.1f)
	}
})
