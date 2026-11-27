package org.jellyfin.androidtv.ui.card

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.VelellaTheme
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProfilePicture
import org.jellyfin.androidtv.ui.base.ProvideTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.item.cardFocusRing
import org.jellyfin.androidtv.util.MenuBuilder
import org.jellyfin.androidtv.util.popupMenu
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.util.showIfNotEmpty

// Velella: the zoom of a focused cover (scale-focus)
private const val CARD_FOCUS_SCALE = 1.06f

@Composable
fun UserCard(
	image: @Composable () -> Unit,
	name: @Composable () -> Unit,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
	val shape: Shape = CircleShape
	val focused by interactionSource.collectIsFocusedAsState()

	// Velella: the avatar of the design system, on surface-3 with the covers' focus ring and zoom, the name muted until focused
	val palette = LocalVelellaPalette.current
	val scale by animateFloatAsState(if (focused) CARD_FOCUS_SCALE else 1f, label = "UserCardFocusScale")

	Column(
		modifier = modifier
			.scale(scale)
			.focusable(interactionSource = interactionSource)
			.clickable(interactionSource = interactionSource, onClick = onClick, indication = null)
	) {
		Box(
			modifier = Modifier
				.aspectRatio(1f)
				.clip(shape)
				.background(palette.surface3)
				.cardFocusRing(focused, palette.accent, shape)
		) {
			image()
		}

		Spacer(Modifier.height(8.dp))

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.basicMarquee(
					iterations = if (focused) Int.MAX_VALUE else 0,
					initialDelayMillis = 0,
				),
			contentAlignment = Alignment.TopCenter,
		) {
			ProvideTextStyle(
				LocalTextStyle.current.copy(
					color = if (focused) palette.ink else palette.inkMuted,
					fontSize = 14.sp,
					fontWeight = FontWeight.Bold,
				)
			) {
				name()
			}
		}
	}
}

class UserCardView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {
	var name by mutableStateOf<String?>(null)
	var image by mutableStateOf<String?>(null)
	private var focused by mutableStateOf(false)

	init {
		isFocusable = true
		descendantFocusability = FOCUS_BLOCK_DESCENDANTS
		if (AndroidVersion.isAtLeastO) defaultFocusHighlightEnabled = false
	}

	fun setPopupMenu(init: MenuBuilder.() -> Unit) {
		setOnLongClickListener {
			popupMenu(context, this, init = init).showIfNotEmpty()
		}
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
		if (super.onKeyUp(keyCode, event)) return true

		// Menu key should show the popup menu
		if (event.keyCode == KeyEvent.KEYCODE_MENU) return performLongClick()

		return false
	}

	override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)

		focused = gainFocus
	}

	@Composable
	override fun Content() {
		val interactionSource = remember { MutableInteractionSource() }

		// Forward focus events to the interaction source
		val focusInteraction = remember { FocusInteraction.Focus() }
		LaunchedEffect(focused) {
			if (focused) interactionSource.emit(focusInteraction)
			else interactionSource.emit(FocusInteraction.Unfocus(focusInteraction))
		}

		// Velella: the theme's colours, the startup screens have no VelellaTheme around them
		VelellaTheme { UserCard(
			image = {
				ProfilePicture(
					url = image,
					iconPadding = PaddingValues(32.dp),
					modifier = Modifier.fillMaxSize()
				)
			},
			name = {
				Text(
					text = name.orEmpty(),
					maxLines = 1
				)
			},
			// Velella: 128dp avatars, 32dp apart
			modifier = Modifier
				.padding(horizontal = 16.dp, vertical = 8.dp)
				.width(128.dp),
			interactionSource = interactionSource,
			// We use our own click handler for views used in presenters
			onClick = {}
		) }
	}
}
