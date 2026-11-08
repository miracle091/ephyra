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
			// 32dp margins on both sides, 12dp between cards
			(width * count + 12f * (count - 1)) shouldBe (896f plusOrMinus 0.5f)
		}
	}

	test("four wide cards are a little shorter than upstream's 126dp") {
		rowCardHeight(screenWidth, ROW_CARDS_WIDE, WIDE_ASPECT_RATIO).value shouldBe (120.9f plusOrMinus 0.1f)
	}

	test("the focused card's 6% zoom stays inside the gap, off the next card") {
		// The widest card: an episode (16:9) in a row of posters, such as "Continue watching"
		val widest = rowCardHeight(screenWidth, ROW_CARDS_POSTER, POSTER_ASPECT_RATIO).value * WIDE_ASPECT_RATIO
		// card_scale_focus (106%) grows the card from its centre, half of it on each side
		(widest * 0.06f / 2 < ROW_ITEM_GAP_DP) shouldBe true
	}
})
