package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import androidx.compose.ui.text.font.FontWeight
import org.jellyfin.androidtv.ui.base.CircularProgressIndicator
import org.jellyfin.androidtv.ui.base.EphyraTheme
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.ButtonBase
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.androidtv.ui.base.button.ButtonStyle
import org.jellyfin.androidtv.util.MenuBuilder
import org.jellyfin.androidtv.util.popupMenu
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.util.showIfNotEmpty

/**
 * The slots of a [ServerButton].
 */
class ServerButtonContent(
	val icon: @Composable () -> Unit,
	val name: @Composable () -> Unit,
	val address: @Composable () -> Unit,
	val version: @Composable () -> Unit,
)

@Composable
fun ServerButton(
	content: ServerButtonContent,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	shape: Shape = ButtonDefaults.Shape,
) {
	ButtonBase(
		onClick = onClick,
		modifier = modifier,
		style = ButtonStyle(shape = shape, colors = ButtonDefaults.colors()),
		interactionSource = interactionSource,
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 16.dp, vertical = 8.dp)
		) {
			content.icon()

			Column(
				modifier = Modifier
					.weight(1f)
					.fillMaxHeight(),
				verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
			) {
				ProvideTextStyle(LocalTextStyle.current.copy(fontSize = 14.sp)) {
					content.name()
				}

				ProvideTextStyle(LocalTextStyle.current.copy(fontSize = 12.sp)) {
					content.address()
				}
			}

			Box(modifier = Modifier.align(Alignment.CenterVertically)) {
				ProvideTextStyle(LocalTextStyle.current.copy(fontSize = 12.sp)) {
					content.version()
				}
			}
		}
	}
}

class ServerButtonView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {
	var name by mutableStateOf("")
	var address by mutableStateOf("")
	var version by mutableStateOf<String?>(null)
	var state by mutableStateOf(State.DEFAULT)
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

		// Ephyra: the theme's colours (the startup screens have no EphyraTheme around them) and a 64dp row with radius-md
		EphyraTheme { ServerButton(
			content = ServerButtonContent(
				icon = {
					when (state) {
						State.DEFAULT -> Icon(
							imageVector = ImageVector.vectorResource(R.drawable.ic_house),
							contentDescription = null,
						)

						State.EDIT -> Icon(
							imageVector = ImageVector.vectorResource(R.drawable.ic_house_edit),
							contentDescription = null,
						)

						State.CONNECTING -> CircularProgressIndicator(
							modifier = Modifier.size(24.dp)
						)

						State.ERROR -> Icon(
							imageVector = ImageVector.vectorResource(R.drawable.ic_error),
							contentDescription = null,
						)
					}
				},
				name = { Text(name, style = LocalTextStyle.current.merge(fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 1) },
				address = { Text(address, style = LocalTextStyle.current.merge(fontSize = 14.sp), maxLines = 1) },
				version = { Text(version.orEmpty()) },
			),
			interactionSource = interactionSource,
			shape = RoundedCornerShape(12.dp),
			modifier = Modifier.height(64.dp),
			// We use our own click handler for the view
			onClick = {}
		) }
	}

	enum class State {
		DEFAULT,
		EDIT,
		CONNECTING,
		ERROR
	}
}
