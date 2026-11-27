package org.jellyfin.androidtv.ui.itemdetail

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.extensions.ticks
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Velella: loads the series' next up episode and shows it as the card beside the buttons; selecting it plays that episode,
 * from where it was left when it is partly watched. An unstarted series gets its first episode, a finished one no card.
 */
fun FullDetailsFragment.loadNextUpCard() {
	lifecycleScope.launch {
		val nextUp = getNextUpEpisode() ?: return@launch
		val row = mDetailsOverviewRow ?: return@launch

		row.nextUpEpisode = nextUp
		row.onNextUpClick = { episode ->
			val position = (episode.userData?.playbackPositionTicks?.ticks ?: Duration.ZERO) - resumePreroll.milliseconds
			play(episode, position.inWholeMilliseconds.coerceAtLeast(0).toInt(), false)
		}
		mDorPresenter?.viewHolder?.setNextUp(row.nextUpEpisode, row.onNextUpClick)
	}
}
