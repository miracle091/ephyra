package org.jellyfin.playback.media3.exoplayer

import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import androidx.annotation.OptIn
import androidx.core.graphics.TypefaceCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView

/** Ephyra: the user's subtitle look, shared by both players. Colours are ARGB. */
data class SubtitleStyle(
	val textColor: Int,
	val backgroundColor: Int,
	val strokeColor: Int,
	val textWeight: Int,
	val textSizeDp: Float,
	val bottomPaddingFraction: Float,
)

private const val ALPHA_SHIFT = 24

/** An outline only when its colour is visible. */
internal fun subtitleEdgeType(strokeColor: Int): Int = when (strokeColor ushr ALPHA_SHIFT) {
	0 -> CaptionStyleCompat.EDGE_TYPE_NONE
	else -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
}

/** Applies [style] to this view's text cues; libass subtitles keep their own styling. */
@OptIn(UnstableApi::class)
fun SubtitleView.applyStyle(style: SubtitleStyle) {
	setStyle(
		CaptionStyleCompat(
			style.textColor,
			style.backgroundColor,
			Color.TRANSPARENT,
			subtitleEdgeType(style.strokeColor),
			style.strokeColor,
			TypefaceCompat.create(context, Typeface.DEFAULT, style.textWeight, false),
		)
	)
	setFixedTextSize(TypedValue.COMPLEX_UNIT_DIP, style.textSizeDp)
	setBottomPaddingFraction(style.bottomPaddingFraction)
}
