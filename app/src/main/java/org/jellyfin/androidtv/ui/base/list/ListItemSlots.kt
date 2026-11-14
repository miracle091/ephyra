package org.jellyfin.androidtv.ui.base.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/** The optional parts around the heading of a list item: the overline, caption, leading, trailing and footer content. */
@Immutable
data class ListItemSlots(
	val overlineContent: (@Composable () -> Unit)? = null,
	val captionContent: (@Composable () -> Unit)? = null,
	val leadingContent: (@Composable () -> Unit)? = null,
	val trailingContent: (@Composable () -> Unit)? = null,
	val footerContent: (@Composable () -> Unit)? = null,
)
