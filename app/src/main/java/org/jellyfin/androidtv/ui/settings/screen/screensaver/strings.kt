package org.jellyfin.androidtv.ui.settings.screen.screensaver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.getQuantityString
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
@Stable
fun getScreensaverTimeoutOptions() = buildList {
	val context = LocalContext.current

	add(30.seconds to context.getQuantityString(R.plurals.seconds, 30))
	add(1.minutes to context.getQuantityString(R.plurals.minutes, 1))
	add(2.5.minutes to context.getQuantityString(R.plurals.minutes, 2.5))
	add(5.minutes to context.getQuantityString(R.plurals.minutes, 5))
	add(10.minutes to context.getQuantityString(R.plurals.minutes, 10))
	add(15.minutes to context.getQuantityString(R.plurals.minutes, 15))
	add(30.minutes to context.getQuantityString(R.plurals.minutes, 30))
}
