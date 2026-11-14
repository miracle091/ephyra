package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.ProvideTextStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ButtonBase(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	style: ButtonStyle = ButtonDefaults.style(),
	interactionSource: MutableInteractionSource? = null,
	content: @Composable BoxScope.() -> Unit
) {
	@Suppress("NAME_SHADOWING")
	val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val pressed by interactionSource.collectIsPressedAsState()

	val colors = style.colors.let {
		when {
			!style.enabled -> it.disabledContainerColor to it.disabledContentColor
			pressed -> it.focusedContainerColor to it.focusedContentColor
			focused -> it.focusedContainerColor to it.focusedContentColor
			else -> it.containerColor to it.contentColor
		}
	}

	ProvideTextStyle(value = JellyfinTheme.typography.default.copy(fontSize = 14.sp, color = colors.second)) {
		Box(
			modifier = modifier
				.combinedClickable(
					interactionSource = interactionSource,
					indication = null,
					enabled = style.enabled,
					role = Role.Button,
					onClick = onClick,
				)
				.background(colors.first, style.shape)
				.clip(style.shape),
			content = content,
		)
	}
}
