package org.jellyfin.androidtv.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.ProvideTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.design.token.SpaceTokens

// Velella: the shared style of the info row (details screen, home spotlight): groups divided by thin vertical lines and the
// official rating in an outlined badge. Dimensions: divider 1dp wide and 14dp high (about 60% of the 14sp..16sp text height)
// in ink-muted at 40% alpha with SpaceTokens.spaceSm (8dp) on both sides; badge with a 1dp ink-muted border, 4dp corners,
// no fill, 11sp bold text in ink-muted, 6dp horizontal and 2dp vertical padding.
private object InfoRowSeparatorStyle {
	val DividerWidth = 1.dp
	val DividerHeight = 14.dp
	const val DividerAlpha = 0.4f
	val DividerSpacing = SpaceTokens.spaceSm
	val BadgeBorderWidth = 1.dp
	val BadgeCornerRadius = 4.dp
}

/** A thin vertical divider between two groups of an info row. */
@Composable
fun InfoRowSeparator(modifier: Modifier = Modifier) {
	val palette = LocalVelellaPalette.current

	Spacer(
		modifier = modifier
			.padding(horizontal = InfoRowSeparatorStyle.DividerSpacing)
			.width(InfoRowSeparatorStyle.DividerWidth)
			.height(InfoRowSeparatorStyle.DividerHeight)
			.alpha(InfoRowSeparatorStyle.DividerAlpha)
			.background(palette.inkMuted)
	)
}

/** The official rating (PG-13, VM14...) in an outlined badge with no fill. */
@Composable
fun InfoRowOutlinedBadge(text: String, modifier: Modifier = Modifier) {
	val palette = LocalVelellaPalette.current

	ProvideTextStyle(TextStyle(color = palette.inkMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = modifier
				.border(
					InfoRowSeparatorStyle.BadgeBorderWidth,
					palette.inkMuted,
					RoundedCornerShape(InfoRowSeparatorStyle.BadgeCornerRadius),
				)
				.padding(horizontal = 6.dp, vertical = 2.dp),
		) {
			Text(text)
		}
	}
}

/** Lays out [groups] in a row with an [InfoRowSeparator] between each pair. */
@Composable
fun InfoRowGroups(groups: List<@Composable () -> Unit>) {
	Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
		groups.forEachIndexed { index, group ->
			if (index > 0) InfoRowSeparator()
			group()
		}
	}
}
