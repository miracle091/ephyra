package org.jellyfin.androidtv.trailer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Velella: plays an item's YouTube trailers through Invidious, in the app, never through YouTube. The streams come from
 * [TrailerResolver]; when one fails to play, the next instance is tried, then the next trailer. Back or the end closes it.
 */
@OptIn(UnstableApi::class)
class TrailerPlayerActivity : FragmentActivity() {
	private val resolver by inject<TrailerResolver>()
	private lateinit var playerView: PlayerView
	private var player: ExoPlayer? = null
	private var videoIds: List<String> = emptyList()
	private var videoIndex = 0
	private val failedInstances = mutableSetOf<String>()
	private var currentInstance: String? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		videoIds = intent.getStringArrayExtra(EXTRA_VIDEO_IDS)?.toList().orEmpty()
		playerView = PlayerView(this).apply {
			keepScreenOn = true
			// The trailer starts without the controls over it; any key shows them
			controllerAutoShow = false
			setShowNextButton(false)
			setShowPreviousButton(false)
		}
		setContentView(playerView)
		playCurrent()
	}

	private fun playCurrent() {
		val videoId = videoIds.getOrNull(videoIndex) ?: return giveUp()
		lifecycleScope.launch {
			val trailer = resolver.resolve(videoId, failedInstances)
			if (trailer == null) {
				// No instance has this one: on to the next trailer, from the start of the instance list
				videoIndex++
				failedInstances.clear()
				playCurrent()
			} else {
				currentInstance = trailer.instance
				play(trailer.streams)
			}
		}
	}

	private fun play(streams: TrailerStreams) {
		val exoPlayer = player ?: ExoPlayer.Builder(this).build().also { created ->
			created.addListener(TrailerListener())
			player = created
			playerView.player = created
		}
		val sources = ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
		val video = sources.createMediaSource(MediaItem.fromUri(streams.videoUrl))
		val source = streams.audioUrl?.let { MergingMediaSource(video, sources.createMediaSource(MediaItem.fromUri(it))) } ?: video
		exoPlayer.setMediaSource(source)
		exoPlayer.prepare()
		exoPlayer.playWhenReady = true
		playerView.requestFocus()
	}

	private fun giveUp() {
		Toast.makeText(this, R.string.velella_trailer_unavailable, Toast.LENGTH_LONG).show()
		finish()
	}

	private inner class TrailerListener : Player.Listener {
		override fun onPlaybackStateChanged(playbackState: Int) {
			if (playbackState == Player.STATE_ENDED) finish()
		}

		override fun onPlayerError(error: PlaybackException) {
			Timber.w(error, "Trailer stream from %s failed, trying the next instance", currentInstance)
			currentInstance?.let(failedInstances::add)
			playCurrent()
		}
	}

	override fun onStop() {
		super.onStop()
		// Leaving the trailer (Home, another app) ends it
		finish()
	}

	override fun onDestroy() {
		super.onDestroy()
		playerView.player = null
		player?.release()
		player = null
	}

	companion object {
		private const val EXTRA_VIDEO_IDS = "video_ids"

		/** The intent that plays the trailers with the YouTube ids [videoIds], in order. */
		fun intent(context: Context, videoIds: List<String>): Intent =
			Intent(context, TrailerPlayerActivity::class.java).putExtra(EXTRA_VIDEO_IDS, videoIds.toTypedArray())
	}
}
