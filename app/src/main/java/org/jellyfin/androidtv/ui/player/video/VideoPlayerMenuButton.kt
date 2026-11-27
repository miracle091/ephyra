package org.jellyfin.androidtv.ui.player.video

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.popover.Popover
import org.jellyfin.androidtv.ui.base.popover.PopoverPlacement

private val MenuWidth = 360.dp
private val MenuMaxHeight = 420.dp
private const val MENU_OFFSET_Y = -5

/** Velella: a menu button's icon from a drawable, for [VideoPlayerMenuButton]. */
@Composable
fun MenuIcon(@DrawableRes icon: Int, contentDescription: String) =
	Icon(imageVector = ImageVector.vectorResource(icon), contentDescription = contentDescription)

/**
 * Velella: a round button of the video player's controls that opens a list to pick one of [options] (audio, subtitles, chapters,
 * quality), like the legacy player's popup menus: the [selected] one is checked and focused when the list opens. [icon] is the
 * button's content ([MenuIcon], or the quality button's badge). A non-null [onOpenRequest] opens the list as a remote key asked
 * (Captions, Audio track) and is then called to clear the request.
 */
@Composable
fun <T> VideoPlayerMenuButton(
	icon: @Composable () -> Unit,
	options: List<PlayerMenuOption<T>>,
	selected: T?,
	onSelect: (T) -> Unit,
	onOpenRequest: (() -> Unit)? = null,
) = Box {
	var expanded by remember { mutableStateOf(false) }
	LaunchedEffect(onOpenRequest != null) {
		if (onOpenRequest != null) {
			expanded = true
			onOpenRequest()
		}
	}

	IconButton(onClick = { expanded = true }) {
		icon()
	}

	Popover(
		expanded = expanded,
		onDismissRequest = { expanded = false },
		placement = PopoverPlacement(alignment = Alignment.TopEnd, offset = DpOffset(0.dp, MENU_OFFSET_Y.dp)),
	) {
		val focusRequester = remember { FocusRequester() }
		val selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)

		Column(
			modifier = Modifier
				.width(MenuWidth)
				.heightIn(max = MenuMaxHeight)
				.verticalScroll(rememberScrollState())
				.padding(4.dp),
		) {
			options.forEachIndexed { index, option ->
				ListButton(
					onClick = {
						expanded = false
						if (option.value != selected) onSelect(option.value)
					},
					headingContent = { Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
					slots = ListItemSlots(trailingContent = { RadioButton(checked = option.value == selected) }),
					modifier = if (index == selectedIndex) Modifier.focusRequester(focusRequester) else Modifier,
				)
			}
		}

		LaunchedEffect(Unit) { focusRequester.requestFocus() }
	}
}
