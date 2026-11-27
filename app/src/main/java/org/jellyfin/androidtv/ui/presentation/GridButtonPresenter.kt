package org.jellyfin.androidtv.ui.presentation

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.leanback.widget.Presenter
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.item.ArtToneCover
import org.jellyfin.androidtv.ui.composable.item.ItemPreview
import org.jellyfin.androidtv.ui.composable.item.cardFocusRing
import org.jellyfin.androidtv.ui.itemhandling.GridButtonBaseRowItem

/**
 * Velella: a grid button (Guide, Recordings, Genres...) drawn like the home's library cards: the `art-*` tone of its label with
 * its icon in the middle, or its picture, and the label centred under the card. The focus ring and zoom are the cards'.
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

			ItemPreview(
				card = {
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
					}
				},
				title = {
					Text(
						text = value.text,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						style = LocalTextStyle.current.merge(textAlign = TextAlign.Center),
						modifier = Modifier
							.width(CARD_WIDTH.dp)
							.then(if (isFocused) Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 0) else Modifier),
					)
				},
			)
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
