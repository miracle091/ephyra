package org.jellyfin.androidtv.ui.background

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.BackgroundService
import org.koin.compose.koinInject

@Composable
private fun AppThemeBackground() {
	val context = LocalContext.current
	val themeBackground = remember(context.theme) {
		val attributes = context.theme.obtainStyledAttributes(intArrayOf(R.attr.defaultBackground))
		val drawable = attributes.getDrawable(0)
		attributes.recycle()

		if (drawable is ColorDrawable) drawable.toBitmap(1, 1).asImageBitmap()
		else drawable?.toBitmap()?.asImageBitmap()
	}

	if (themeBackground != null) {
		Image(
			bitmap = themeBackground,
			contentDescription = null,
			alignment = Alignment.Center,
			contentScale = ContentScale.Crop,
			modifier = Modifier.fillMaxSize()
		)
	} else {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Black)
		)
	}
}

@Composable
fun AppBackground() {
	val backgroundService = koinInject<BackgroundService>()
	val enabled by backgroundService.enabled.collectAsState()

	// Ephyra: the theme's plain background on every screen, no item backdrops
	if (enabled) AppThemeBackground()
}
