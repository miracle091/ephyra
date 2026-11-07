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
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.design.token.EphyraColorTokens

/**
 * Ephyra: the focus ring of a cover in the Ephyra design system, a solid [ring] line of 3dp with a 2dp [gap] inside it. It is
 * drawn inside the card, so the card keeps its size.
 */
fun Modifier.cardFocusRing(focused: Boolean, ring: Color, gap: Color, shape: Shape): Modifier =
	if (!focused) this
	else this
		.border(3.dp, ring, shape)
		.border(5.dp, gap, shape)

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
