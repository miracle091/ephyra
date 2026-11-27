package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.playback.OsdHeader
import org.jellyfin.androidtv.ui.playback.PlayInfo
import org.jellyfin.androidtv.ui.player.base.PlayerHeader
import org.jellyfin.androidtv.util.sdk.getSeasonEpisodeName
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * Velella: the video player's header as in the legacy player (OsdHeader): the series and episode number (the genres for anything
 * else) as the overline over the name, then how the server delivers the stream and why (PlayInfo).
 */
@Composable
@Stable
fun VideoPlayerHeader(
	item: BaseItemDto?,
	stream: PlayableMediaStream?,
	// Velella: a live channel shows only the badge and the channel here; the programme and the play method sit above the buttons
	live: LiveChannelInfo? = null,
) {
	val palette = LocalVelellaPalette.current
	val context = LocalContext.current

	// The legacy header: the text keeps 24dp from the clock, the clock sits 8dp lower than the title block
	PlayerHeader(clockModifier = Modifier.padding(top = 8.dp), spacing = 24.dp) {
		if (live != null) {
			LiveChannelLine(live)
			return@PlayerHeader
		}

		if (item != null) {
			val overline = remember(item) { OsdHeader.overlineOf(item, item.getSeasonEpisodeName(context)) }
			if (overline.isNotBlank()) {
				Text(
					text = overline.uppercase(),
					overflow = TextOverflow.Ellipsis,
					maxLines = 1,
					style = LocalTextStyle.current.merge(
						color = palette.inkMuted,
						fontSize = 11.sp,
						fontWeight = FontWeight.Bold,
						letterSpacing = 0.08.em,
					),
				)
			}

			Text(
				text = OsdHeader.titleOf(item).orEmpty(),
				overflow = TextOverflow.Ellipsis,
				maxLines = 1,
				style = LocalTextStyle.current.merge(color = palette.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold),
				modifier = Modifier.padding(top = 4.dp),
			)
		}

		if (stream != null) {
			val playInfo = remember(stream) { PlayInfo.describe(context, stream.conversionMethod.playMethod, stream.url) }
			Text(
				text = playInfo,
				style = LocalTextStyle.current.merge(color = palette.inkMuted, fontSize = 14.sp),
				modifier = Modifier.padding(top = 4.dp),
			)
		}
	}
}
