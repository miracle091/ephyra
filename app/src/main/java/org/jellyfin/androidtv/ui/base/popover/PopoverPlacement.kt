package org.jellyfin.androidtv.ui.base.popover

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpOffset

/** Where a [Popover] opens: the [alignment] relative to its anchor, moved by [offset]. */
@Immutable
data class PopoverPlacement(
	val alignment: Alignment = Alignment.TopStart,
	val offset: DpOffset = DpOffset.Zero,
)
