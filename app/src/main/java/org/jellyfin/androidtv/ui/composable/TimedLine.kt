package org.jellyfin.androidtv.ui.composable

import kotlin.time.Duration

data class TimedLine(
	val timestamp: Duration,
	val text: String,
)
