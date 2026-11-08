package org.jellyfin.androidtv.ui.startup.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.EphyraMonoFontFamily
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.androidtv.ui.base.Text

private const val GROUP_SIZE = 3
private val TileShape = RoundedCornerShape(12.dp)
private val TileEdge = Color.White.copy(alpha = 0.10f)

/**
 * Ephyra: a Quick Connect code with each character in its own tile, in groups of three ("420 420"), so it reads at a glance
 * from the sofa.
 */
@Composable
fun QuickConnectCode(code: String, modifier: Modifier = Modifier) {
	val palette = LocalEphyraPalette.current

	Row(
		modifier = modifier,
		horizontalArrangement = Arrangement.spacedBy(24.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		code.chunked(GROUP_SIZE).forEach { group ->
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				group.forEach { character ->
					Box(
						modifier = Modifier
							.size(width = 56.dp, height = 72.dp)
							.background(palette.surface2, TileShape)
							.border(1.dp, TileEdge, TileShape),
						contentAlignment = Alignment.Center,
					) {
						Text(
							text = character.toString(),
							color = palette.ink,
							fontSize = 36.sp,
							fontWeight = FontWeight.Bold,
							fontFamily = EphyraMonoFontFamily, // Ephyra
						)
					}
				}
			}
		}
	}
}
