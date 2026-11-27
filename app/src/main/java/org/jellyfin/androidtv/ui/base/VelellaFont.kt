package org.jellyfin.androidtv.ui.base

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.jellyfin.androidtv.R

// Velella: Atkinson Hyperlegible Next (text) and Mono (codes), SIL OFL 1.1. Each is one variable file with a wght axis.
private val FontWeights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resId: Int) = FontFamily(
	FontWeights.map { weight ->
		Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
	}
)

val VelellaFontFamily: FontFamily = variableFamily(R.font.atkinson_hyperlegible_next)
val VelellaMonoFontFamily: FontFamily = variableFamily(R.font.atkinson_hyperlegible_mono)
