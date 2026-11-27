package org.jellyfin.androidtv.ui.player.video

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import org.jellyfin.androidtv.R
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlaybackFailure
import org.jellyfin.playback.core.queue.queue
import timber.log.Timber

/**
 * Velella: when the new player leaves on its own, as the legacy one did: it tells the user about each retry with a heavier stream,
 * closes after too many errors or when the server has no stream for the last item, and closes when the queue ends. Only while
 * the screen is started: stopping the player clears the queue, which must not close it a second time.
 */
@Composable
fun VideoPlayerExits(
	playbackManager: PlaybackManager,
	onClose: () -> Unit,
) {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	LaunchedEffect(playbackManager, lifecycleOwner) {
		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			playbackManager.failures.collect { failure ->
				val message = when (failure) {
					is PlaybackFailure.Retrying -> R.string.player_error
					is PlaybackFailure.GaveUp -> R.string.too_many_errors
					is PlaybackFailure.Unplayable ->
						if (failure.hasNext) R.string.velella_player_unplayable_skipped else R.string.velella_player_unplayable
				}
				Toast.makeText(context, message, Toast.LENGTH_LONG).show()
				if (closesPlayer(failure)) onClose()
			}
		}
	}

	LaunchedEffect(playbackManager, lifecycleOwner) {
		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			var played = false
			playbackManager.queue.entry.collect { entry ->
				if (entry != null) {
					played = true
				} else if (played) {
					Timber.i("Queue ended, closing the player")
					onClose()
				}
			}
		}
	}
}

/** Velella: whether a failure leaves nothing to play, so the player closes. */
fun closesPlayer(failure: PlaybackFailure): Boolean = when (failure) {
	is PlaybackFailure.Retrying -> false
	is PlaybackFailure.GaveUp -> true
	is PlaybackFailure.Unplayable -> !failure.hasNext
}
