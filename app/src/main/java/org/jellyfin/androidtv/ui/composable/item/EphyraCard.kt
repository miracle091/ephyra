package org.jellyfin.androidtv.ui.composable.item

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.design.token.EphyraColorTokens

/**
 * Ephyra: the focus ring of a cover, a solid [ring] line of 1.5dp along its edge. It is drawn inside the card, so the card keeps
 * its size.
 */
fun Modifier.cardFocusRing(focused: Boolean, ring: Color, shape: Shape): Modifier =
	if (!focused) this
	else this.border(1.5.dp, ring, shape)

/** Ephyra: wide cards (16:9) shown in a home row at once. */
const val ROW_CARDS_WIDE = 4

/** Ephyra: posters (2:3) shown in a home row at once. */
const val ROW_CARDS_POSTER = 8

/** Ephyra: wide cards shown at once in the small "My media" row. */
const val ROW_CARDS_WIDE_SMALL = 6

const val WIDE_ASPECT_RATIO = 16f / 9f
const val POSTER_ASPECT_RATIO = 2f / 3f

// The browsePaddingStart/End of the Ephyra themes and Leanback's item spacing
private val RowSideMargin = 32.dp
private val RowItemGap = 8.dp

/**
 * Ephyra: the height of the cards of a row, so that [count] cards of [aspectRatio] fill a screen [screenWidth] wide edge to edge,
 * without an empty strip or half a card at the end.
 */
fun rowCardHeight(screenWidth: Dp, count: Int, aspectRatio: Float): Dp =
	(screenWidth - RowSideMargin * 2 - RowItemGap * (count - 1)) / count / aspectRatio

/** Ephyra: the `surface-1` fill a cover shows while its picture loads. */
@Composable
fun CardPlaceholder() {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(LocalEphyraPalette.current.surface1),
	)
}

/** Ephyra: the `art-*` tone of a cover without a picture, picked from its title so the same title always gets the same tone. */
fun artToneFor(title: String?): Color = EphyraColorTokens.art[(title.orEmpty().hashCode()).mod(EphyraColorTokens.art.size)]

/**
 * Ephyra: a cover without a picture, filled with the `art-*` tone of its [title]; the title itself shows in the card's caption.
 * A cover without a title shows [iconRes] instead.
 */
@Composable
fun ArtToneCover(
	title: String?,
	@DrawableRes iconRes: Int,
	modifier: Modifier = Modifier,
) {
	Box(
		contentAlignment = Alignment.Center,
		modifier = modifier
			.fillMaxSize()
			.background(artToneFor(title)),
	) {
		if (title.isNullOrBlank()) {
			Image(
				painter = painterResource(iconRes),
				contentDescription = null,
				modifier = Modifier.fillMaxSize(0.4f),
			)
		}
	}
}
