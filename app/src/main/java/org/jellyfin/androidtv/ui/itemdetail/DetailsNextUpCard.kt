package org.jellyfin.androidtv.ui.itemdetail

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.EphyraTheme
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.item.ArtToneCover
import org.jellyfin.androidtv.ui.composable.item.CardProgressBar
import org.jellyfin.androidtv.ui.composable.item.cardFocusRing
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.sdk.getSeasonEpisodeName
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject

private const val CARD_FOCUS_SCALE = 1.06f
private const val SCRIM_ALPHA = 0.8f
private const val SCRIM_START = 0.45f
// As tall as the info pairs and buttons beside it, so the card adds no height to the details
private val CardWidth = 160.dp
private val CardHeight = 90.dp // 16:9

/**
 * Ephyra: the "Next up" card of a series' details, a 16:9 card beside the action buttons with the next episode's picture, a
 * "Next: S1:E3 · Title" caption and the progress bar of a partly watched episode. It is a focusable view like the covers, with
 * the same accent ring and zoom; a click is handled by the view's click listener. Hidden while [episode] is null.
 */
class DetailsNextUpCard @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {
	private var focused by mutableStateOf(false)
	private var _episode by mutableStateOf<BaseItemDto?>(null)

	var episode: BaseItemDto?
		get() = _episode
		set(value) {
			_episode = value
			visibility = if (value == null) GONE else VISIBLE
			isFocusable = value != null
		}

	init {
		isFocusable = false
		visibility = GONE
		descendantFocusability = FOCUS_BLOCK_DESCENDANTS
		defaultFocusHighlightEnabled = false
	}

	override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)

		focused = gainFocus
	}

	@Composable
	override fun Content() {
		val item = _episode ?: return

		EphyraTheme { NextUpCard(item, focused) }
	}
}

@Composable
private fun NextUpCard(item: BaseItemDto, focused: Boolean) {
	val api = koinInject<ApiClient>()
	val palette = LocalEphyraPalette.current
	val context = LocalContext.current
	val shape = JellyfinTheme.shapes.medium
	val scale by animateFloatAsState(if (focused) CARD_FOCUS_SCALE else 1f, label = "NextUpCardFocusScale")
	val maxWidth = with(LocalDensity.current) { CardWidth.roundToPx() * 2 }

	val title = DetailsNextUp.title(item.getSeasonEpisodeName(context), item.name)
	val caption = stringResource(R.string.ephyra_next_up_card, title)
	val image = item.itemImages[ImageType.PRIMARY] ?: item.itemImages[ImageType.THUMB]
	val progress = DetailsNextUp.progress(item)

	// The padding leaves room for the zoom inside the view's bounds
	Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
		Box(
			modifier = Modifier
				.size(CardWidth, CardHeight)
				.scale(scale)
				.clip(shape)
				.background(palette.surface1)
				.cardFocusRing(focused, palette.accent, shape)
		) {
			if (image != null) {
				AsyncImage(
					model = image.getUrl(api, maxWidth = maxWidth),
					contentDescription = null,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize(),
				)
			} else {
				ArtToneCover(item.name, R.drawable.ic_play, alwaysShowIcon = true)
			}

			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(Brush.verticalGradient(SCRIM_START to Color.Transparent, 1f to Color.Black.copy(alpha = SCRIM_ALPHA))),
			)

			Text(
				text = caption,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				color = palette.ink,
				fontSize = 14.sp,
				fontWeight = FontWeight.Bold,
				modifier = Modifier
					.align(Alignment.BottomStart)
					.fillMaxWidth()
					.padding(start = 10.dp, end = 10.dp, bottom = if (progress != null) 10.dp else 8.dp)
					.then(if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 0) else Modifier),
			)

			if (progress != null) {
				CardProgressBar(progress = progress, modifier = Modifier.align(Alignment.BottomStart))
			}
		}
	}
}
