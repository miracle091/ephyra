package org.jellyfin.androidtv.ui.settings.screen.customization.subtitle

import androidx.compose.ui.graphics.Color

private const val ARGB_WHITE = 0xFFFFFFFFL
private const val ARGB_BLACK = 0xFF000000L
private const val ARGB_GREY = 0xFF7F7F7FL
private const val ARGB_RED = 0xFFC80000L
private const val ARGB_GREEN = 0xFF00C800L
private const val ARGB_BLUE = 0xFF0000C8L
private const val ARGB_YELLOW = 0xFFEEDC00L
private const val ARGB_MAGENTA = 0xFFD60080L
private const val ARGB_CYAN = 0xFF009FDAL

private val SubtitleColorPresets = setOf(
	Color(color = ARGB_WHITE),
	Color(color = ARGB_BLACK),
	Color(color = ARGB_GREY),
	Color(color = ARGB_RED),
	Color(color = ARGB_GREEN),
	Color(color = ARGB_BLUE),
	Color(color = ARGB_YELLOW),
	Color(color = ARGB_MAGENTA),
	Color(color = ARGB_CYAN),
)

val SubtitleTextColorPresets = SubtitleColorPresets
val SubtitleBackgroundColorPresets = setOf(Color.Transparent) + SubtitleColorPresets
val SubtitleTextStrokeColorPresets = setOf(Color.Transparent) + SubtitleColorPresets
