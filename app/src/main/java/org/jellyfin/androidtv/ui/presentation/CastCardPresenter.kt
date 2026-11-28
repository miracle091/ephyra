package org.jellyfin.androidtv.ui.presentation

import android.view.KeyEvent
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.leanback.widget.Presenter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.base.VelellaTheme
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.item.ArtToneCover
import org.jellyfin.androidtv.ui.composable.item.cardFocusRing
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.getActivity
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.compose.koinInject

// Large enough to recognise faces from the sofa, small enough that a row still shows several people; portrait like the person
// cards elsewhere (7:9), as tall as the square pictures were, so the row keeps its height
private val CastImageHeight = 120.dp
private val CastImageWidth = CastImageHeight * ImageHelper.ASPECT_RATIO_7_9.toFloat()
private const val MARQUEE_DELAY_MS = 1500
private val MarqueeFadeWidth = 8.dp

/**
 * Velella: the compact cast card of the details screens: a 120dp-tall portrait picture (7:9) with rounded corners and, under
 * it, the person's name and role in two small lines. Focus ring and click behaviour are the covers': the row handles the click
 * and the ring is drawn here.
 */
class CastCardPresenter : Presenter() {
	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = ComposeView(parent.context).apply {
			setParentCompositionContext(parent.findViewTreeCompositionContext())
			setViewTreeLifecycleOwner(parent.findViewTreeLifecycleOwner())
			setViewTreeSavedStateRegistryOwner(parent.findViewTreeSavedStateRegistryOwner())
			isFocusable = true
			isFocusableInTouchMode = true

			setOnLongClickListener {
				context.getActivity()?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU)) ?: false
			}
		}

		return CastViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		if (viewHolder !is CastViewHolder || item !is BaseRowItem) return

		viewHolder.bind(item)
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		if (viewHolder is CastViewHolder) viewHolder.unbind()
	}

	private class CastViewHolder(composeView: ComposeView) : ViewHolder(composeView) {
		private val item = MutableStateFlow<BaseRowItem?>(null)
		private val focused = MutableStateFlow(false)

		init {
			composeView.setContent {
				val current by item.collectAsState()
				val isFocused by focused.collectAsState()

				VelellaTheme { current?.let { CastCard(it, isFocused) } }
			}

			focused.value = view.isFocused
			composeView.setOnFocusChangeListener { _, hasFocus -> focused.value = hasFocus }
		}

		fun bind(value: BaseRowItem) {
			item.value = value
			focused.value = view.isFocused
		}

		fun unbind() {
			item.value = null
			focused.value = false
		}
	}
}

@Composable
private fun CastCard(item: BaseRowItem, focused: Boolean) {
	val api = koinInject<ApiClient>()
	val context = LocalContext.current
	val palette = LocalVelellaPalette.current
	val shape = JellyfinTheme.shapes.medium
	val imageHeight = with(LocalDensity.current) { CastImageHeight.roundToPx() }

	val name = item.getFullName(context)
	val role = item.getSubText(context)
	val image = item.getImage(ImageType.POSTER)

	Column(modifier = Modifier.width(CastImageWidth)) {
		Box(
			modifier = Modifier
				.size(width = CastImageWidth, height = CastImageHeight)
				.clip(shape)
				.background(palette.surface1)
				.cardFocusRing(focused, palette.accent, shape)
		) {
			if (image != null) {
				AsyncImage(
					model = image.getUrl(api, maxHeight = imageHeight * 2),
					contentDescription = null,
					contentScale = ContentScale.Crop,
					// Portraits: keep the faces in the frame
					alignment = Alignment.TopCenter,
					modifier = Modifier.fillMaxSize(),
				)
			} else {
				ArtToneCover(name, R.drawable.ic_user)
			}
		}

		Spacer(Modifier.height(6.dp))

		CastLine(name.orEmpty(), LocalTextStyle.current.merge(color = palette.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold))

		if (!role.isNullOrBlank()) {
			CastLine(role, LocalTextStyle.current.merge(color = palette.inkMuted, fontSize = 12.sp))
		}
	}
}

/**
 * A name or role line: on every card, focused or not, a line longer than the card scrolls by itself, with faded edges so the
 * moving texts of neighbouring cards stay apart; a line that fits stays still and whole.
 */
@Composable
private fun CastLine(text: String, style: TextStyle) {
	BoxWithConstraints {
		val measurer = rememberTextMeasurer()
		val overflows = remember(text, style, constraints.maxWidth) {
			measurer.measure(text, style, maxLines = 1, softWrap = false).size.width > constraints.maxWidth
		}

		Text(
			text = text,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			style = style,
			modifier = if (overflows) {
				Modifier
					.fadedEdges()
					.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = MARQUEE_DELAY_MS)
			} else {
				Modifier
			},
		)
	}
}

private fun Modifier.fadedEdges() = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
	.drawWithContent {
		drawContent()
		// Only lines wider than the card get here, so the card is always much wider than the two fades
		val edge = MarqueeFadeWidth.toPx() / size.width
		drawRect(
			brush = Brush.horizontalGradient(0f to Color.Transparent, edge to Color.Black, 1f - edge to Color.Black, 1f to Color.Transparent),
			blendMode = BlendMode.DstIn,
		)
	}
