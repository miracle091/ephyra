package org.jellyfin.androidtv.ui.base.button

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape

/**
 * How a button looks and whether it reacts: its [shape], its [colors] and whether it is [enabled].
 * A disabled button uses the disabled colors of [colors].
 */
@Immutable
data class ButtonStyle(
	val shape: Shape,
	val colors: ButtonColors,
	val enabled: Boolean = true,
)
