package org.jellyfin.androidtv.ui.composable.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme

/**
 * Velella: the one progress bar of cards, shared by every surface that shows watched progress. It hugs the bottom edge of the card,
 * full width, with square ends (the card clips it to its rounded corners): a black 38% track and a fill in the theme's accent.
 * The accent is [JellyfinTheme.colorScheme]'s `rangeControlFill`, the design system's `accent` of the user's theme. The View twin
 * is `@drawable/velella_card_progress` (accent from `?android:attr/colorAccent`), which uses the same height and track colour.
 */
@Composable
fun CardProgressBar(
	progress: Float,
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier
			.fillMaxWidth()
			.height(dimensionResource(R.dimen.velella_card_progress_height))
			.background(colorResource(R.color.velella_card_progress_track))
	) {
		Box(
			modifier = Modifier
				.fillMaxHeight()
				.fillMaxWidth(progress.coerceIn(0f, 1f))
				.background(JellyfinTheme.colorScheme.rangeControlFill)
		)
	}
}
