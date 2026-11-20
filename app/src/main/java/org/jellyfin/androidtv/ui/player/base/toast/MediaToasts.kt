package org.jellyfin.androidtv.ui.player.base.toast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text

@Composable
fun MediaToasts(mediaToastRegistry: MediaToastRegistry) {
	val data by mediaToastRegistry.current.collectAsState()

	MediaToast(
		visible = data != null,
		icon = {
			data?.let { data ->
				val label = data.label
				if (label == null) {
					Icon(
						imageVector = ImageVector.vectorResource(data.icon),
						contentDescription = null,
						modifier = Modifier.fillMaxSize()
					)
				} else {
					// Ephyra: a smaller icon with the label under it
					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.Center,
						modifier = Modifier.fillMaxSize(),
					) {
						Icon(
							imageVector = ImageVector.vectorResource(data.icon),
							contentDescription = null,
							modifier = Modifier.size(32.dp)
						)
						Text(
							text = label,
							maxLines = 1,
							style = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
						)
					}
				}
			}
		},
		progress = data?.progress,
	)
}
