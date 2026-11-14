package org.jellyfin.androidtv.ui.base.list

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
fun ListButton(
	onClick: () -> Unit,
	headingContent: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	style: ListControlStyle = ListControlDefaults.style(),
	slots: ListItemSlots = ListItemSlots(),
) {
	val interactionSource = remember { MutableInteractionSource() }

	ListControl(
		headingContent = headingContent,
		modifier = modifier.combinedClickable(
			interactionSource = interactionSource,
			indication = null,
			enabled = style.enabled,
			role = Role.Button,
			onClick = onClick,
		),
		interactionSource = interactionSource,
		style = style,
		slots = slots,
	)
}
