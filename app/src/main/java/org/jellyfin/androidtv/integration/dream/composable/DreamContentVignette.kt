package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush

private const val VIGNETTE_END_STOP = 0.95f

private val vignetteBrush = object : ShaderBrush() {
	override fun createShader(size: Size): Shader = RadialGradientShader(
		colors = listOf(
			Color.Black.copy(alpha = 0.2f),
			Color.Black.copy(alpha = 0.7f),
		),
		center = size.center,
		radius = maxOf(size.width, size.height) / 2f,
		colorStops = listOf(0f, VIGNETTE_END_STOP)
	)
}

@Composable
fun DreamContentVignette(
	modifier: Modifier = Modifier
) = Box(
	modifier = modifier
		.background(vignetteBrush)
		.fillMaxSize()
)
