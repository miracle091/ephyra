package org.jellyfin.androidtv.ui.player.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.livetv.TvManager
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.InteractionTrackerViewModel
import org.jellyfin.androidtv.ui.base.BaseScreen
import org.jellyfin.androidtv.ui.playback.TrackPreselection
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.ui.playback.playedMediaSource
import org.jellyfin.androidtv.ui.playback.rewrite.RewriteMediaManager
import org.jellyfin.androidtv.ui.player.display.VideoModeRequest
import org.jellyfin.androidtv.ui.player.display.resetDisplayMode
import org.jellyfin.androidtv.ui.player.display.switchDisplayMode
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.mediastream.mediaStreamFlow
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.QueueService
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.extensions.ticks
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class VideoPlayerFragment : Fragment() {
	companion object {
		const val EXTRA_POSITION: String = "position"
	}

	private val videoQueueManager by inject<VideoQueueManager>()
	private val playbackManager by inject<PlaybackManager>()
	private val api by inject<ApiClient>()
	private val userPreferences by inject<UserPreferences>()
	private val trackPreselection by inject<TrackPreselection>()
	private val interactionTracker by inject<InteractionTrackerViewModel>()
	private val dataRefreshService by inject<DataRefreshService>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Create a queue from the items added to the legacy video queue; Velella: starting at the launcher's item and resume point,
		// applied when that entry's stream starts (a seek here would land before the stream is known)
		startQueue(arguments?.getInt(EXTRA_POSITION)?.milliseconds)

		// Pause player until the initial resume
		playbackManager.state.pause()

		// Velella: next up and still watching, as in the legacy player: the queue holds at the end of an entry while one is asked
		queueService.advanceAtEnd = advanceAtEnd
		// Velella: the prompt lasts until another entry starts; closing the player (no entry) keeps it, see playerOverlay
		playbackManager.queue.entry
			.filterNotNull()
			.onEach { entry ->
				endPrompt.value = EndOfEntryPrompt.NONE
				switchingChannel.value = false
				// Velella: as the legacy player, so the details screen below shows the episode played last
				entry.baseItem?.let { dataRefreshService.lastPlayedItem = it }
				// Velella: as the legacy player, for the previous channel button, the channel bar and the guide's first focus
				entry.baseItem?.takeIf { it.type == BaseItemKind.TV_CHANNEL }?.let { TvManager.setLastLiveTvChannel(it.id) }
			}
			.launchIn(lifecycleScope)
	}

	/** The queue of the items added to the legacy video queue, from the launcher's item; [startPosition] for its first entry. */
	private fun startQueue(startPosition: Duration?) {
		val startIndex = videoQueueManager.getCurrentMediaPosition().coerceAtLeast(0)
		// Velella: the tracks picked on the details screen, used once
		val items = videoQueueManager.getCurrentVideoQueue()
		val startTracks = items.getOrNull(startIndex)?.let { item -> trackPreselection.take(item.id, item.playedMediaSource?.id) }
		val queueSupplier = RewriteMediaManager.BaseItemQueueSupplier(
			api,
			items,
			false,
			startIndex,
			startPosition,
			startTracks,
		)
		Timber.i("Created a queue with ${queueSupplier.items.size} items, starting at $startIndex")
		playbackManager.queue.clear()
		playbackManager.queue.addSupplier(queueSupplier, startIndex)
	}

	/** Velella: tunes to another live TV channel in this player, as the legacy player's channel bar and guide do (playChannel). */
	private fun switchChannel(channelId: UUID) {
		val current = playbackManager.queue.entry.value?.baseItem
		if (current?.id == channelId) return

		viewLifecycleOwner.lifecycleScope.launch {
			val channel = try {
				withContext(Dispatchers.IO) { api.libraryApi.getItem(channelId).content }
			} catch (error: CancellationException) {
				throw error
			} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
				Timber.w(error, "Unable to get channel $channelId")
				Toast.makeText(requireContext(), R.string.msg_video_playback_error, Toast.LENGTH_LONG).show()
				return@launch
			}

			// The player was left (Home, the guide) while the channel was asked for: it must not start playing in the background
			if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@launch
			Timber.i("Switching to channel ${channel.id}")
			switchingChannel.value = true
			videoQueueManager.setCurrentVideoQueue(listOf(channel))
			videoQueueManager.setCurrentMediaPosition(0)
			startQueue(null)
			playbackManager.state.unpause()
		}
	}

	private val queueService get() = requireNotNull(playbackManager.getService<QueueService>()) { "No queue service" }
	private val endPrompt = MutableStateFlow(EndOfEntryPrompt.NONE)
	private val switchingChannel = MutableStateFlow(false)
	private val liveTvSwitch = LiveTvSwitch(switchingChannel, ::switchChannel)
	private val advanceAtEnd: (QueueEntry, QueueEntry) -> Boolean = { ended, _ ->
		val endedItem = ended.baseItem
		interactionTracker.onEpisodeWatched(endedItem?.runTimeTicks?.ticks?.inWholeMilliseconds ?: 0)
		val prompt = endOfEntryPrompt(
			endedType = endedItem?.type,
			nextUp = userPreferences[UserPreferences.nextUpBehavior],
			stillWatching = userPreferences[UserPreferences.stillWatchingBehavior],
			stillWatchingDue = interactionTracker.getShowStillWatching(),
		)
		Timber.i("Entry ended, prompt: $prompt")
		endPrompt.value = prompt
		prompt == EndOfEntryPrompt.NONE
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		BaseScreen {
			VideoPlayerScreen(endPrompt = endPrompt, liveTvSwitch = liveTvSwitch)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		// Velella: the display mode follows each entry's video (frame rate and size of the source), as in the legacy player
		val behavior = userPreferences[UserPreferences.refreshRateSwitchingBehavior]
		@OptIn(ExperimentalCoroutinesApi::class)
		playbackManager.queue.entry
			.flatMapLatest { entry -> entry?.mediaStreamFlow?.onStart { emit(entry.mediaStream) } ?: flowOf(null) }
			.mapNotNull { stream -> stream?.tracks?.filterIsInstance<MediaStreamVideoTrack>()?.firstOrNull() }
			.mapNotNull { track -> track.frameRate?.let { VideoModeRequest(track.width, track.height, it) } }
			.distinctUntilChanged()
			.onEach { request -> requireActivity().switchDisplayMode(request, behavior) }
			.launchIn(viewLifecycleOwner.lifecycleScope)
	}

	override fun onPause() {
		super.onPause()

		playbackManager.state.pause()
	}

	override fun onResume() {
		super.onResume()

		playbackManager.state.unpause()
	}

	override fun onStop() {
		super.onStop()

		playbackManager.state.stop()
		// Velella: as the legacy player's stop report, so the screens below fetch the item's progress and watched state again
		dataRefreshService.markPlaybackStopped(dataRefreshService.lastPlayedItem?.type, Instant.now())
	}

	override fun onDestroy() {
		super.onDestroy()

		// Velella: give the display its previous mode back
		activity?.resetDisplayMode()
		// Velella: other players (music, a newer video player) move on by themselves; leave a newer player's hold in place
		if (queueService.advanceAtEnd === advanceAtEnd) queueService.advanceAtEnd = { _, _ -> true }
	}
}
