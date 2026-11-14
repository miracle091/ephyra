package org.jellyfin.androidtv.ui.settings.screen.screensaver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.getQuantityString
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val HALF_MINUTE_IN_SECONDS = 30
private const val TWO_AND_A_HALF_MINUTES = 2.5
private const val FIVE_MINUTES = 5
private const val TEN_MINUTES = 10
private const val FIFTEEN_MINUTES = 15
private const val THIRTY_MINUTES = 30

@Composable
@Stable
fun getScreensaverTimeoutOptions() = buildList {
	val context = LocalContext.current

	add(HALF_MINUTE_IN_SECONDS.seconds to context.getQuantityString(R.plurals.seconds, HALF_MINUTE_IN_SECONDS))
	add(1.minutes to context.getQuantityString(R.plurals.minutes, 1))
	add(TWO_AND_A_HALF_MINUTES.minutes to context.getQuantityString(R.plurals.minutes, TWO_AND_A_HALF_MINUTES))
	add(FIVE_MINUTES.minutes to context.getQuantityString(R.plurals.minutes, FIVE_MINUTES))
	add(TEN_MINUTES.minutes to context.getQuantityString(R.plurals.minutes, TEN_MINUTES))
	add(FIFTEEN_MINUTES.minutes to context.getQuantityString(R.plurals.minutes, FIFTEEN_MINUTES))
	add(THIRTY_MINUTES.minutes to context.getQuantityString(R.plurals.minutes, THIRTY_MINUTES))
}
