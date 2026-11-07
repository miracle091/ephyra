package org.jellyfin.androidtv.ui.playback

import android.widget.ImageView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.isActive
import org.jellyfin.playback.media3.exoplayer.AudioLevel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.EphyraTheme
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.blurHashPainter
import org.jellyfin.androidtv.ui.composable.LyricsDtoBox
import org.jellyfin.androidtv.ui.composable.modifier.fadingEdges
import org.jellyfin.androidtv.ui.composable.rememberPlayerPositionInfo
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.androidtv.ui.player.base.PlayerSeekbar
import org.jellyfin.androidtv.util.apiclient.albumPrimaryImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.jellyfin.lyrics.lyrics
import org.jellyfin.playback.jellyfin.lyrics.lyricsFlow
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.baseItemFlow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject

/** Ephyra: the line under the song title, "Artist · Year". */
fun nowPlayingSubtitle(item: BaseItemDto, artistName: String): String =
	listOfNotNull(artistName.ifBlank { null }, item.productionYear?.toString()).joinToString(" · ")

private const val BACKDROP_ZOOM_MS = 20_000
private const val BACKDROP_DRIFT_MS = 27_000
private const val BACKDROP_SCALE_MIN = 1.3f
private const val BACKDROP_SCALE_MAX = 1.5f
private const val BACKDROP_DRIFT_X = 0.05f
private const val BACKDROP_DRIFT_Y = 0.04f
private const val BACKDROP_GRADIENT_START_ALPHA = 0.3f
private const val BACKDROP_BLURHASH_SIZE = 32

// The loudness of music is mostly 0.05 to 0.4 RMS: the gain spreads it over 0 to 1, the smoothing keeps the pulse soft
private const val BACKDROP_PULSE_GAIN = 2.5f
private const val BACKDROP_PULSE_SMOOTHING = 0.18f
private const val BACKDROP_PULSE_SCALE = 0.15f
private const val BACKDROP_PULSE_SCRIM_LIFT = 0.35f

/**
 * Ephyra: the player's moving backdrop, the cover's blurhash (already a soft blur, also on Android 11 where Compose can't blur)
 * slowly zooming and drifting under the theme's scrim and a gradient into the background colour. While music plays it breathes
 * with the loudness ([AudioLevel]): it grows and the scrim lifts a little on loud passages. The pulse is read once a frame and
 * only redraws the backdrop's layers, never recomposes.
 */
fun initializeBackdropView(
	backdropView: ComposeView,
	playbackManager: PlaybackManager,
) {
	backdropView.setContent {
		EphyraTheme {
			val palette = LocalEphyraPalette.current
			val entry by rememberQueueEntry(playbackManager)
			val baseItem = entry?.run { baseItemFlow.collectAsState(baseItem).value }
			val cover = baseItem?.itemImages[ImageType.PRIMARY] ?: baseItem?.albumPrimaryImage ?: baseItem?.parentImages[ImageType.PRIMARY]

			val transition = rememberInfiniteTransition(label = "backdrop")
			val scale by transition.animateFloat(
				initialValue = BACKDROP_SCALE_MIN,
				targetValue = BACKDROP_SCALE_MAX,
				animationSpec = infiniteRepeatable(tween(BACKDROP_ZOOM_MS, easing = LinearEasing), RepeatMode.Reverse),
				label = "backdropScale",
			)
			val drift by transition.animateFloat(
				initialValue = -1f,
				targetValue = 1f,
				animationSpec = infiniteRepeatable(tween(BACKDROP_DRIFT_MS, easing = LinearEasing), RepeatMode.Reverse),
				label = "backdropDrift",
			)

			val playState by remember { playbackManager.state.playState }.collectAsState()
			val pulse = remember { Animatable(0f) }
			LaunchedEffect(playState) {
				if (playState != PlayState.PLAYING) {
					pulse.animateTo(0f)
					return@LaunchedEffect
				}
				while (isActive) {
					withFrameNanos { }
					val target = (AudioLevel.level.value * BACKDROP_PULSE_GAIN).coerceIn(0f, 1f)
					pulse.snapTo(pulse.value + (target - pulse.value) * BACKDROP_PULSE_SMOOTHING)
				}
			}

			Box(Modifier.fillMaxSize()) {
				AnimatedContent(cover?.blurHash, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "backdropCover") { blurHash ->
					if (blurHash != null) Image(
						painter = blurHashPainter(blurHash, IntSize(BACKDROP_BLURHASH_SIZE, BACKDROP_BLURHASH_SIZE)),
						contentDescription = null,
						contentScale = ContentScale.Crop,
						modifier = Modifier
							.fillMaxSize()
							.graphicsLayer {
								val breathing = scale + pulse.value * BACKDROP_PULSE_SCALE
								scaleX = breathing
								scaleY = breathing
								translationX = drift * size.width * BACKDROP_DRIFT_X
								translationY = -drift * size.height * BACKDROP_DRIFT_Y
							},
					)
				}

				Box(
					Modifier
						.fillMaxSize()
						.drawBehind {
							drawRect(palette.scrim.copy(alpha = palette.scrim.alpha * (1f - pulse.value * BACKDROP_PULSE_SCRIM_LIFT)))
						}
				)
				Box(
					Modifier
						.fillMaxSize()
						.background(Brush.horizontalGradient(listOf(palette.bg.copy(alpha = BACKDROP_GRADIENT_START_ALPHA), palette.bg)))
				)
			}
		}
	}
}

fun initializePlayerProgress(
	playerProgress: ComposeView,
	playbackManager: PlaybackManager,
) {
	playerProgress.setContent {
		Box(
			modifier = Modifier
				.padding(horizontal = 10.dp, vertical = 20.dp)
		) {
			PlayerSeekbar(
				playbackManager = playbackManager,
				modifier = Modifier
					.fillMaxWidth()
					.height(4.dp)
			)
		}
	}
}

fun initializePreviewView(
	lyricsView: ComposeView,
	playbackManager: PlaybackManager,
) {
	lyricsView.setContent {
		val api = koinInject<ApiClient>()
		val entry by rememberQueueEntry(playbackManager)
		val baseItem = entry?.run { baseItemFlow.collectAsState(baseItem).value }
		val lyrics = entry?.run { lyricsFlow.collectAsState(lyrics) }?.value
		val cover = baseItem?.itemImages[ImageType.PRIMARY] ?: baseItem?.albumPrimaryImage ?: baseItem?.parentImages[ImageType.PRIMARY]

		// Show track/album art when available and fade it out when lyrics are displayed on top
		val coverViewAlpha by animateFloatAsState(
			label = "coverViewAlpha",
			targetValue = if (lyrics == null) 1f else 0.2f,
		)

		AnimatedContent(cover) { cover ->
			if (cover != null) {
				// Ephyra: the cover fills the square with radius-lg, as in the design system's player
				Box(
					modifier = Modifier
						.fillMaxSize()
						.clip(RoundedCornerShape(20.dp))
						.background(Color.Black)
				) {
					AsyncImage(
						url = cover.getUrl(api),
						blurHash = cover.blurHash,
						aspectRatio = 1f,
						scaleType = ImageView.ScaleType.CENTER_CROP,
						modifier = Modifier
							.fillMaxSize()
							.alpha(coverViewAlpha)
					)
				}
			} else if (lyrics == null) {
				// "placeholder" image
				Icon(ImageVector.vectorResource(R.drawable.ic_album), contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
			}
		}

		// Display lyrics overlay
		if (lyrics != null) {
			val playState by remember { playbackManager.state.playState }.collectAsState()
			val positionInfo by rememberPlayerPositionInfo(playbackManager)

			LyricsDtoBox(
				lyricDto = lyrics,
				currentTimestamp = positionInfo.active,
				duration = positionInfo.duration,
				paused = playState != PlayState.PLAYING,
				fontSize = 12.sp,
				color = Color.White,
				modifier = Modifier
					.fillMaxSize()
					.fadingEdges(vertical = 50.dp)
					.padding(horizontal = 15.dp),
			)
		}
	}
}
