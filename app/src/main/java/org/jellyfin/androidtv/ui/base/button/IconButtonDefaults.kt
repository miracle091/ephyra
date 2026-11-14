package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object IconButtonDefaults {
	val Shape: Shape = ButtonDefaults.Shape
	val ContentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 10.dp)

	@ReadOnlyComposable
	@Composable
	fun colors() = ButtonDefaults.colors()
}
