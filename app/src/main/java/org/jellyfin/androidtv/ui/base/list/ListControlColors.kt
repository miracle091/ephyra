package org.jellyfin.androidtv.ui.base.list

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ListControlColors(
	val containerColor: Color,
	val focusedContainerColor: Color,
)

/** How a list control looks and whether it reacts: its [colors] and whether it is [enabled]. */
@Immutable
data class ListControlStyle(
	val colors: ListControlColors,
	val enabled: Boolean = true,
)
