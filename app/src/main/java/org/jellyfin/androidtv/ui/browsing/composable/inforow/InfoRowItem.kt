package org.jellyfin.androidtv.ui.browsing.composable.inforow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle

/**
 * A single item in the [BaseItemInfoRow].
 */
@Composable
fun InfoRowItem(
	// Icon options
	icon: ImageVector? = null,
	iconTint: Color? = null,
	contentDescription: String?,
	// Styling
	colors: Pair<Color, Color> = InfoRowColors.Transparent,
	// Content
	content: @Composable RowScope.() -> Unit,
) {
	// Velella: plain items in ink-muted like the home's meta line, neutral badges as the design system's badge
	val palette = LocalVelellaPalette.current
	val (backgroundColor, foregroundColor) = when (colors) {
		InfoRowColors.Transparent -> Color.Transparent to palette.inkMuted
		InfoRowColors.Default -> palette.surface2 to palette.inkMuted
		else -> colors
	}

	val modifier = when {
		backgroundColor.alpha > 0f -> Modifier
			.height(20.dp)
			.background(backgroundColor, RoundedCornerShape(4.dp))
			.padding(horizontal = 6.dp)

		else -> Modifier
	}

	ProvideTextStyle(
		value = TextStyle(
			color = foregroundColor,
			fontSize = if (backgroundColor.alpha > 0f) 11.sp else 14.sp,
			fontWeight = if (backgroundColor.alpha > 0f) FontWeight.Bold else FontWeight.W500,
			letterSpacing = if (backgroundColor.alpha > 0f) 0.06.em else TextUnit.Unspecified,
		)
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(3.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = modifier.fillMaxHeight(),
		) {
			if (icon != null) {
				Icon(
					imageVector = icon,
					contentDescription = contentDescription,
					modifier = Modifier.size(if (backgroundColor.alpha > 0f) 16.dp else 18.dp),
					tint = iconTint ?: LocalTextStyle.current.color,
				)
			}

			content()
		}
	}
}
