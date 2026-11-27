package org.jellyfin.androidtv.ui.presentation

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.leanback.widget.Presenter
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.item.ArtToneCover
import org.jellyfin.androidtv.ui.composable.item.cardFocusRing
import org.jellyfin.androidtv.ui.itemhandling.GridButtonBaseRowItem

// Velella: the design system's card overlay shade, rgba(5, 8, 20), as on the library cards
private val BUTTON_SHADE = Color(red = 5, green = 8, blue = 20)

// Velella: where the shade starts and how opaque it is at the bottom, as on the other cards
private const val SHADE_START = 0.4f
private const val SHADE_END_ALPHA = 0.86f

/**
 * Velella: a grid button (Guide, Recordings, Genres...) drawn as a wide library card: the `art-*` tone of its label with its icon
 * in the middle, or its picture, and the label in bold on the card's bottom shade. The focus ring and zoom are the cards'.
 */
class GridButtonPresenter : Presenter() {
	private class ComposeViewWrapper(composeView: ComposeView) : FrameLayout(composeView.context) {
		init {
			isFocusable = true
			isFocusableInTouchMode = true
			descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
			addView(composeView)
		}

		// Hack to prevent Compose crash with leanback presenters
		override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
			if (isAttachedToWindow) super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			else setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
		}
	}

	inner class ViewHolder(
		private val composeView: ComposeView,
	) : Presenter.ViewHolder(ComposeViewWrapper(composeView)) {
		private val focused = MutableStateFlow(false)

		init {
			view.setOnFocusChangeListener { _, hasFocus -> focused.value = hasFocus }
		}

		fun bind(value: GridButton) = composeView.setContent {
			val isFocused by focused.collectAsState()
			val shape = JellyfinTheme.shapes.medium

			Box(
				modifier = Modifier
					.size(CARD_WIDTH.dp, CARD_HEIGHT.dp)
					.clip(shape)
					.cardFocusRing(focused = isFocused, ring = LocalVelellaPalette.current.accent, shape = shape)
			) {
				if (value.imageRes != null) {
					Image(
						painter = painterResource(value.imageRes),
						contentDescription = null,
						contentScale = ContentScale.Crop,
						modifier = Modifier.fillMaxSize(),
					)
				} else {
					ArtToneCover(value.text, value.iconRes ?: R.drawable.ic_folder, alwaysShowIcon = true)
				}

				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(
							Brush.verticalGradient(
								SHADE_START to BUTTON_SHADE.copy(alpha = 0f),
								1f to BUTTON_SHADE.copy(alpha = SHADE_END_ALPHA),
							),
						),
				)

				Text(
					text = value.text,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					style = LocalTextStyle.current.merge(
						color = LocalVelellaPalette.current.ink,
						fontSize = 18.sp,
						fontWeight = FontWeight.Bold,
						lineHeight = 22.sp,
					),
					modifier = Modifier
						.align(Alignment.BottomStart)
						.fillMaxWidth()
						.then(if (isFocused) Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 0) else Modifier)
						.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
				)
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = ComposeView(parent.context).apply {
			isFocusable = true
		}

		return ViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
		if (viewHolder !is ViewHolder) return

		when (item) {
			is GridButtonBaseRowItem -> viewHolder.bind(item.gridButton)
			is GridButton -> viewHolder.bind(item)
		}
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) = Unit
	override fun onViewAttachedToWindow(viewHolder: Presenter.ViewHolder) = Unit

	private companion object {
		// 16:9, the size of the small "My media" cards on a 960dp screen
		const val CARD_WIDTH = 192
		const val CARD_HEIGHT = 108
	}
}
