package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.integration.dream.formatPlaybackTime
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.integration.dream.pausedVideoProgress
import org.jellyfin.androidtv.ui.base.EphyraTheme
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.playback.OsdHeader
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.parentBackdropImages
import org.jellyfin.androidtv.util.getTimeFormatter
import org.jellyfin.androidtv.util.sdk.getSeasonEpisodeName
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.compose.koinInject
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

// Against burn-in: after a while the picture dims further and the text moves a little
private val BurnInDelay = 10.minutes
private const val PICTURE_ALPHA = 0.7f
private const val PICTURE_ALPHA_LATE = 0.35f
private val TextStart = 32.dp
private val TextStartLate = 56.dp
private const val LATE_MOVE_MS = 4_000

// The video player's measures (vlc_player_interface.xml, lb_playback_transport_controls_row.xml): 32dp sides, 27dp top and bottom
private val SideMargin = 32.dp
private val SafeY = 27.dp
// The header's width, as the home spotlight's text block
private val HeaderWidth = 520.dp

// The player's times (TextAppearance.Ephyra.OsdTime): 14sp with even-width digits
private val TimeStyle = TextStyle(fontSize = 14.sp, fontFeatureSettings = "tnum")

/**
 * Ephyra: the screensaver over a video left paused, laid out as the video player's controls: the header at the top left, the
 * progress with the times at the bottom. The title's backdrop is dimmed; any key closes the screensaver and brings the player back.
 */
@Composable
fun DreamContentPaused(content: DreamContent.VideoPaused) = EphyraTheme {
	val palette = LocalEphyraPalette.current
	var late by remember { mutableStateOf(false) }
	LaunchedEffect(content) {
		delay(BurnInDelay)
		late = true
	}
	val pictureAlpha by animateFloatAsState(if (late) PICTURE_ALPHA_LATE else PICTURE_ALPHA, tween(LATE_MOVE_MS), label = "PausePicture")
	val textStart by animateDpAsState(if (late) TextStartLate else TextStart, tween(LATE_MOVE_MS), label = "PauseText")

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(palette.bg),
	) {
		PausedPicture(content.item, Modifier.alpha(pictureAlpha))
		// The player's shades: one under the header, one under the progress
		Box(
			Modifier
				.fillMaxSize()
				.background(
					Brush.verticalGradient(
						0f to palette.bg.copy(alpha = 0.8f),
						0.3f to Color.Transparent,
						0.6f to Color.Transparent,
						1f to palette.bg,
					)
				)
		)

		val context = LocalContext.current
		val progress = remember(content) { pausedVideoProgress(content.positionMs, content.durationMs, LocalTime.now()) }
		val endsAt = remember(progress) { context.getTimeFormatter().format(progress.endsAt) }

		PausedHeader(
			item = content.item,
			endsAt = endsAt,
			modifier = Modifier
				.align(Alignment.TopStart)
				.padding(start = textStart, top = SafeY, end = SideMargin),
		)

		PausedProgress(
			content = content,
			fraction = progress.fraction,
			modifier = Modifier
				.align(Alignment.BottomCenter)
				.padding(start = textStart, end = SideMargin, bottom = SafeY),
		)

		// The way back, centred on the screen on the times' line (same type and bottom margin), between the position and the duration
		Text(
			text = stringResource(R.string.ephyra_paused_hint),
			style = TimeStyle,
			color = palette.inkFaint,
			textAlign = TextAlign.Center,
			maxLines = 1,
			modifier = Modifier
				.align(Alignment.BottomCenter)
				.padding(bottom = SafeY),
		)
	}
}

@Composable
private fun PausedPicture(item: BaseItemDto, modifier: Modifier) {
	val api = koinInject<ApiClient>()
	val image = item.itemBackdropImages.firstOrNull() ?: item.parentBackdropImages.firstOrNull() ?: return
	val maxWidth = with(LocalDensity.current) { 960.dp.roundToPx() }
	AsyncImage(
		model = image.getUrl(api, maxWidth = maxWidth),
		contentDescription = null,
		contentScale = ContentScale.Crop,
		modifier = modifier.fillMaxSize(),
	)
}

/** The player's header (OsdHeader): the overline, the title, then the pause in place of the play method and the end time. */
@Composable
private fun PausedHeader(item: BaseItemDto, endsAt: String, modifier: Modifier) {
	val palette = LocalEphyraPalette.current
	val context = LocalContext.current
	val overline = remember(item) { OsdHeader.overlineOf(item, item.getSeasonEpisodeName(context)) }

	Column(modifier = modifier.width(HeaderWidth)) {
		if (overline.isNotBlank()) {
			Text(
				text = overline.uppercase(),
				fontSize = 11.sp,
				fontWeight = FontWeight.Bold,
				letterSpacing = 0.08.em,
				color = palette.inkMuted,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		}
		Text(
			text = item.name.orEmpty(),
			fontSize = 28.sp,
			fontWeight = FontWeight.Bold,
			color = palette.ink,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.padding(top = 4.dp),
		)
		Row(
			horizontalArrangement = Arrangement.spacedBy(6.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.padding(top = 4.dp),
		) {
			Icon(painterResource(R.drawable.ic_pause), contentDescription = null, tint = palette.accent, modifier = Modifier.size(14.dp))
			Text(text = stringResource(R.string.ephyra_paused), fontSize = 14.sp, color = palette.accent)
		}
		Row(
			horizontalArrangement = Arrangement.spacedBy(6.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.padding(top = 4.dp),
		) {
			Icon(painterResource(R.drawable.ic_time), contentDescription = null, tint = palette.inkMuted, modifier = Modifier.size(14.dp))
			Text(text = stringResource(R.string.lbl_playback_control_ends, endsAt), fontSize = 14.sp, color = palette.inkMuted)
		}
	}
}

/** The player's seek bar with the position and the duration under it. */
@Composable
private fun PausedProgress(content: DreamContent.VideoPaused, fraction: Float, modifier: Modifier) {
	val palette = LocalEphyraPalette.current

	Column(modifier = modifier.fillMaxWidth()) {
		PausedProgressBar(fraction)
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 8.dp),
		) {
			Text(formatPlaybackTime(content.positionMs), style = TimeStyle, color = palette.inkMuted)
			Spacer(Modifier.weight(1f))
			Text(formatPlaybackTime(content.durationMs), style = TimeStyle, color = palette.inkMuted)
		}
	}
}

@Composable
private fun PausedProgressBar(fraction: Float) {
	val palette = LocalEphyraPalette.current
	val shape = RoundedCornerShape(2.dp)
	Box(
		Modifier
			.fillMaxWidth()
			.height(4.dp)
			.background(palette.inkFaint, shape)
	) {
		Box(
			Modifier
				.fillMaxWidth(fraction)
				.height(4.dp)
				.background(palette.accent, shape)
		)
	}
}
