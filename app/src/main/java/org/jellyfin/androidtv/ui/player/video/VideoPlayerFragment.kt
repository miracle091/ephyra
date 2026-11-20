package org.jellyfin.androidtv.ui.player.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.jellyfin.androidtv.preference.UserPreferences
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
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.android.ext.android.inject
import timber.log.Timber
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

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Create a queue from the items added to the legacy video queue; Ephyra: starting at the launcher's item and resume point,
		// applied when that entry's stream starts (a seek here would land before the stream is known)
		val startIndex = videoQueueManager.getCurrentMediaPosition().coerceAtLeast(0)
		val startPosition = arguments?.getInt(EXTRA_POSITION)?.milliseconds
		// Ephyra: the tracks picked on the details screen, used once
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

		// Pause player until the initial resume
		playbackManager.state.pause()
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		BaseScreen {
			VideoPlayerScreen()
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		// Ephyra: the display mode follows each entry's video (frame rate and size of the source), as in the legacy player
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
	}

	override fun onDestroy() {
		super.onDestroy()

		// Ephyra: give the display its previous mode back
		activity?.resetDisplayMode()
	}
}
