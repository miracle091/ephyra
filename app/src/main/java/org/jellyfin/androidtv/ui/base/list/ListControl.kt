package org.jellyfin.androidtv.ui.base.list

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalShapes

private const val DISABLED_ALPHA = 0.4f

@Composable
fun ListControl(
	headingContent: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	interactionSource: InteractionSource? = null,
	style: ListControlStyle = ListControlDefaults.style(),
	slots: ListItemSlots = ListItemSlots(),
) {
	val colors = style.colors
	val enabled = style.enabled
	@Suppress("NAME_SHADOWING")
	val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val pressed by interactionSource.collectIsPressedAsState()

	val backgroundColor = when {
		!enabled -> colors.containerColor
		pressed -> colors.focusedContainerColor
		focused -> colors.focusedContainerColor
		else -> colors.containerColor
	}

	Box(
		modifier = modifier
			.fillMaxWidth()
			.background(backgroundColor, LocalShapes.current.medium)
			.clip(LocalShapes.current.medium)
			.alpha(if (enabled) 1f else DISABLED_ALPHA),
	) {
		ListItemContent(
			headingContent = headingContent,
			headingStyle = JellyfinTheme.typography.listHeadline
				.copy(color = JellyfinTheme.colorScheme.listHeadline),
			slots = slots,
		)
	}
}
