package org.jellyfin.androidtv.integration.dream.composable

import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.ui.base.SeekbarDefaults
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.AsyncImageSource
import org.jellyfin.androidtv.ui.composable.LyricsDtoBox
import org.jellyfin.androidtv.ui.composable.LyricsPosition
import org.jellyfin.androidtv.ui.composable.blurHashPainter
import org.jellyfin.androidtv.ui.composable.modifier.fadingEdges
import org.jellyfin.androidtv.ui.composable.modifier.overscan
import org.jellyfin.androidtv.ui.composable.rememberPlayerPositionInfo
import org.jellyfin.androidtv.ui.player.base.PlayerSeekbar
import org.jellyfin.androidtv.util.apiclient.albumPrimaryImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.jellyfin.lyrics.lyrics
import org.jellyfin.playback.jellyfin.lyrics.lyricsFlow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.LyricDto
import org.koin.compose.koinInject

private const val BLUR_HASH_SIZE = 32
private const val LYRICS_FONT_SIZE = 22
private const val LYRICS_FADING_EDGE = 250
private const val LYRICS_PADDING = 50
private const val ARTWORK_SIZE = 128
private const val ARTWORK_CORNER_RADIUS = 5
private const val TITLE_FONT_SIZE = 26
private const val ARTIST_FONT_SIZE = 18
private const val ARTIST_COLOR_COMPONENT = 0.8f

@Composable
fun DreamContentNowPlaying(
	content: DreamContent.NowPlaying,
) = Box(
	modifier = Modifier.fillMaxSize(),
) {
	val api = koinInject<ApiClient>()
	val playbackManager = koinInject<PlaybackManager>()
	val lyrics = content.entry.run { lyricsFlow.collectAsState(lyrics) }.value

	val primaryImage = content.item.itemImages[ImageType.PRIMARY]
		?: content.item.albumPrimaryImage
		?: content.item.parentImages[ImageType.PRIMARY]

	// Background
	if (primaryImage?.blurHash != null) {
		Image(
			painter = blurHashPainter(primaryImage.blurHash, IntSize(BLUR_HASH_SIZE, BLUR_HASH_SIZE)),
			contentDescription = null,
			alignment = Alignment.Center,
			contentScale = ContentScale.Crop,
			modifier = Modifier.fillMaxSize(),
		)

		DreamContentVignette()
	}

	// Lyrics overlay (on top of background)
	if (lyrics != null) {
		DreamLyricsOverlay(lyrics = lyrics, playbackManager = playbackManager)
	}

	// Metadata overlay (includes title / progress)
	Row(
		verticalAlignment = Alignment.Bottom,
		horizontalArrangement = Arrangement.spacedBy(20.dp),
		modifier = Modifier
			.align(Alignment.BottomStart)
			.overscan(),
	) {
		if (primaryImage != null) {
			AsyncImage(
				source = AsyncImageSource(url = primaryImage.getUrl(api), blurHash = primaryImage.blurHash),
				scaleType = ImageView.ScaleType.CENTER_CROP,
				modifier = Modifier
					.size(ARTWORK_SIZE.dp)
					.clip(RoundedCornerShape(ARTWORK_CORNER_RADIUS.dp))
			)
		}

		DreamMetadata(item = content.item, playbackManager = playbackManager)
	}
}

@Composable
private fun DreamLyricsOverlay(
	lyrics: LyricDto,
	playbackManager: PlaybackManager,
) {
	val playState by remember { playbackManager.state.playState }.collectAsState()
	val positionInfo by rememberPlayerPositionInfo(playbackManager)

	LyricsDtoBox(
		lyricDto = lyrics,
		position = LyricsPosition(
			timestamp = positionInfo.active,
			duration = positionInfo.duration,
			paused = playState != PlayState.PLAYING,
		),
		textStyle = LocalTextStyle.current.merge(color = Color.White, fontSize = LYRICS_FONT_SIZE.sp),
		modifier = Modifier
			.fillMaxSize()
			.fadingEdges(vertical = LYRICS_FADING_EDGE.dp)
			.padding(horizontal = LYRICS_PADDING.dp),
	)
}

@Composable
private fun DreamMetadata(
	item: BaseItemDto,
	playbackManager: PlaybackManager,
) {
	Column(
		modifier = Modifier
			.padding(bottom = 10.dp)
	) {
		Text(
			text = item.name.orEmpty(),
			style = TextStyle(
				color = Color.White,
				fontSize = TITLE_FONT_SIZE.sp,
			),
		)

		Text(
			text = item.run {
				val artistNames = artists.orEmpty()
				val albumArtistNames = albumArtists?.mapNotNull { it.name }.orEmpty()

				when {
					artistNames.isNotEmpty() -> artistNames
					albumArtistNames.isNotEmpty() -> albumArtistNames
					else -> listOfNotNull(albumArtist)
				}.joinToString(", ")
			},
			style = TextStyle(
				color = Color(
					red = ARTIST_COLOR_COMPONENT,
					green = ARTIST_COLOR_COMPONENT,
					blue = ARTIST_COLOR_COMPONENT,
				),
				fontSize = ARTIST_FONT_SIZE.sp,
			),
		)

		Spacer(modifier = Modifier.height(10.dp))

		PlayerSeekbar(
			playbackManager = playbackManager,
			colors = SeekbarDefaults.colors(
				backgroundColor = Color.White.copy(alpha = 0.2f),
				progressColor = Color.White,
				bufferColor = Color.Transparent,
			),
			modifier = Modifier
				.fillMaxWidth()
				.height(4.dp)
		)
	}
}
