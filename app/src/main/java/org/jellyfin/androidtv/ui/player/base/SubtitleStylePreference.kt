package org.jellyfin.androidtv.ui.player.base

import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.playback.media3.exoplayer.SubtitleStyle

/** Ephyra: the subtitle look from the user's settings, for both players. Colours are stored as ARGB longs. */
fun UserPreferences.subtitleStyle(): SubtitleStyle = SubtitleStyle(
	textColor = this[UserPreferences.subtitlesTextColor].toInt(),
	backgroundColor = this[UserPreferences.subtitlesBackgroundColor].toInt(),
	strokeColor = this[UserPreferences.subtitleTextStrokeColor].toInt(),
	textWeight = this[UserPreferences.subtitlesTextWeight],
	textSizeDp = this[UserPreferences.subtitlesTextSize],
	bottomPaddingFraction = this[UserPreferences.subtitlesOffsetPosition],
)
