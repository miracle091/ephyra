package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.TimeUtils

private const val SECONDS_PER_MINUTE = 60
private const val THREE_SECONDS = 3
private const val FIVE_SECONDS = 5
private const val SEVEN_SECONDS = 7
private const val TEN_SECONDS = 10
private const val TWENTY_SECONDS = 20
private const val THIRTY_SECONDS = 30
private const val ONE_MINUTE = SECONDS_PER_MINUTE
private const val TWO_MINUTES = 2 * SECONDS_PER_MINUTE
private const val FIVE_MINUTES = 5 * SECONDS_PER_MINUTE

@Composable
@Stable
fun getResumeSubtractDurationOptions(): Map<String, String> {
	val context = LocalContext.current
	return setOf(
		0, // Disable
		THREE_SECONDS, FIVE_SECONDS, SEVEN_SECONDS,
		TEN_SECONDS, TWENTY_SECONDS, THIRTY_SECONDS, ONE_MINUTE,
		TWO_MINUTES, FIVE_MINUTES,
	).associate {
		val value = if (it == 0) stringResource(R.string.lbl_none)
		else TimeUtils.formatSeconds(context, it)

		it.toString() to value
	}
}
