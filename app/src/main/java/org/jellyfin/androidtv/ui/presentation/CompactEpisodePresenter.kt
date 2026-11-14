package org.jellyfin.androidtv.ui.presentation

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.AsyncImageSource
import org.jellyfin.androidtv.ui.composable.item.ArtToneCover
import org.jellyfin.androidtv.ui.composable.item.CardPlaceholder
import org.jellyfin.androidtv.ui.composable.item.ItemCard
import org.jellyfin.androidtv.ui.composable.item.ItemCardBaseItemOverlay
import org.jellyfin.androidtv.ui.composable.item.WIDE_ASPECT_RATIO
import org.jellyfin.androidtv.ui.composable.item.cardFocusRing
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.getActivity
import org.jellyfin.design.Tokens
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.compose.koinInject

// The browsePaddingStart/End of the Ephyra themes, and room for the focus ring on both sides
private const val ROW_SIDE_MARGIN_DP = 32
private const val ROW_RING_ROOM_DP = 8

private val ThumbnailWidth = 240.dp
private val ThumbnailHeight = ThumbnailWidth / WIDE_ASPECT_RATIO

private const val OVERVIEW_MAX_LINES = 3

/** Ephyra: a row of the season screen that holds one episode of the compact list (see [CompactEpisodePresenter]). */
class CompactEpisodeRow(header: HeaderItem?, adapter: ObjectAdapter) : ListRow(header, adapter)

/**
 * Ephyra: an episode as a horizontal row of the compact episode list: the picture on the left (with its progress bar and the
 * spoiler blur) and on the right the numbered title, the meta line and the plot. A row of the list holds one episode, so up and
 * down on the D-pad move between episodes.
 */
class CompactEpisodePresenter : Presenter() {
	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val metrics = parent.resources.displayMetrics
		val sideMargin = ((ROW_SIDE_MARGIN_DP + ROW_RING_ROOM_DP) * metrics.density).toInt()

		val view = ComposeView(parent.context).apply {
			setParentCompositionContext(parent.findViewTreeCompositionContext())
			setViewTreeLifecycleOwner(parent.findViewTreeLifecycleOwner())
			setViewTreeSavedStateRegistryOwner(parent.findViewTreeSavedStateRegistryOwner())
			isFocusable = true
			isFocusableInTouchMode = true
			layoutParams = ViewGroup.LayoutParams(metrics.widthPixels - sideMargin * 2, ViewGroup.LayoutParams.WRAP_CONTENT)

			setOnLongClickListener {
				context.getActivity()?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU)) ?: false
			}
		}

		return CompactViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		if (viewHolder !is CompactViewHolder || item !is BaseRowItem) return

		viewHolder.bind(item)
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		if (viewHolder is CompactViewHolder) viewHolder.unbind()
	}

	private class CompactViewHolder(composeView: ComposeView) : Presenter.ViewHolder(composeView) {
		private val _item = MutableStateFlow<BaseRowItem?>(null)
		private val _focused = MutableStateFlow(false)

		init {
			composeView.setContent {
				val item by _item.collectAsState()
				val focused by _focused.collectAsState()

				CompactEpisodeContent(item = item, focused = focused)
			}

			_focused.value = view.isFocused
			composeView.onFocusChangeListener = { _, focused -> _focused.value = focused }
		}

		fun bind(item: BaseRowItem) {
			_item.value = item
			_focused.value = view.isFocused
		}

		fun unbind() {
			_item.value = null
			_focused.value = false
		}
	}
}

@Composable
private fun CompactEpisodeContent(item: BaseRowItem?, focused: Boolean) {
	val baseItem = item?.baseItem ?: return
	val context = LocalContext.current
	val palette = LocalEphyraPalette.current
	val shape = JellyfinTheme.shapes.medium

	val title = remember(baseItem) { compactEpisodeTitle(baseItem.indexNumber, baseItem.indexNumberEnd, baseItem.name) }
	// The end time is worked out each time the row is bound, so it is fresh when the screen is shown again
	val meta = remember(baseItem, context) { baseItem.episodeMetaLine(context) }
	val overview = baseItem.overview?.takeIf { it.isNotBlank() }
	val image = remember(item) { item.getImage(ImageType.POSTER) }

	Row(
		horizontalArrangement = Arrangement.spacedBy(Tokens.Space.spaceMd),
		verticalAlignment = Alignment.Top,
		modifier = Modifier
			.fillMaxWidth()
			.clip(shape)
			.background(if (focused) palette.surface1 else Color.Transparent)
			.cardFocusRing(focused = focused, ring = palette.accent, shape = shape)
			.padding(Tokens.Space.spaceSm),
	) {
		CompactEpisodeThumbnail(baseItem, image)
		CompactEpisodeTexts(title, meta, overview, Modifier.weight(1f))
	}
}

@Composable
private fun CompactEpisodeThumbnail(baseItem: BaseItemDto, image: JellyfinImage?) {
	val density = LocalDensity.current

	ItemCard(
		modifier = Modifier.size(ThumbnailWidth, ThumbnailHeight),
		image = {
			CardPlaceholder()
			if (image != null && hidesEpisodeSpoiler(baseItem, image)) {
				EpisodeSpoilerImage(image, WIDE_ASPECT_RATIO, Modifier.fillMaxSize())
			} else if (image != null) {
				val api = koinInject<ApiClient>()
				AsyncImage(
					source = AsyncImageSource(
						url = image.getUrl(
							api,
							maxWidth = with(density) { ThumbnailWidth.roundToPx() },
							maxHeight = with(density) { ThumbnailHeight.roundToPx() },
						),
						blurHash = image.blurHash,
					),
					aspectRatio = WIDE_ASPECT_RATIO,
					scaleType = ImageView.ScaleType.CENTER_CROP,
					modifier = Modifier.fillMaxSize(),
				)
			} else {
				ArtToneCover(baseItem.name, R.drawable.ic_tv)
			}
		},
		// The shared overlay draws the watched badge and the progress bar
		overlay = { ItemCardBaseItemOverlay(item = baseItem) },
	)
}

@Composable
private fun CompactEpisodeTexts(title: String, meta: String?, overview: String?, modifier: Modifier) {
	val palette = LocalEphyraPalette.current

	Column(
		verticalArrangement = Arrangement.spacedBy(Tokens.Space.spaceXs),
		modifier = modifier,
	) {
		Text(
			text = title,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			style = LocalTextStyle.current.merge(color = palette.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold),
		)

		if (meta != null) {
			Text(
				text = meta,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				style = LocalTextStyle.current.merge(color = palette.inkMuted, fontSize = 14.sp),
			)
		}

		if (overview != null) {
			Text(
				text = overview,
				maxLines = OVERVIEW_MAX_LINES,
				overflow = TextOverflow.Ellipsis,
				style = LocalTextStyle.current.merge(color = palette.ink, fontSize = 14.sp, lineHeight = 20.sp),
			)
		}
	}
}
