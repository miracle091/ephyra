package org.jellyfin.androidtv.benchmark

import android.content.Context
import android.media.MediaCodecList
import android.os.SystemClock
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.jellyfin.playback.media3.exoplayer.shield.ShieldLoadControl
import org.jellyfin.playback.media3.exoplayer.shield.ShieldMediaCodecSelector
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Ephyra (debug builds only): plays one clip straight from its URL, with the decoder choice of the app's players
 * ([ShieldMediaCodecSelector], decoder fallback, FFmpeg only where the platform has no decoder), and measures the result.
 * Runs on the main thread, as ExoPlayer requires.
 */
@OptIn(UnstableApi::class)
class DecoderBenchmark(
	private val context: Context,
	private val playDuration: Duration = DEFAULT_PLAY_SECONDS.seconds,
	private val startTimeout: Duration = 30.seconds,
) {
	suspend fun run(clip: BenchmarkClip, surfaceView: SurfaceView): DecoderResult {
		val renderersFactory = DefaultRenderersFactory(context)
			.setMediaCodecSelector(ShieldMediaCodecSelector())
			.setEnableDecoderFallback(true)
			.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
		val player = ExoPlayer.Builder(context, renderersFactory)
			.setLoadControl(ShieldLoadControl.build(context))
			.build()
		val listener = Listener()

		return try {
			player.addAnalyticsListener(listener)
			player.setVideoSurfaceView(surfaceView)
			player.volume = 0f
			player.setMediaItem(MediaItem.fromUri(clip.url))
			val startedAt = SystemClock.elapsedRealtime()
			player.prepare()
			player.play()

			val started = withTimeoutOrNull(startTimeout) { listener.firstFrameOrError.await() }
			val firstFrameMs = SystemClock.elapsedRealtime() - startedAt
			when {
				started == null -> listener.result(clip, error = "no first frame after $startTimeout")
				started is PlaybackException -> listener.result(clip, error = started.errorCodeName)
				else -> {
					withTimeoutOrNull(playDuration) { listener.error.await() }
					val counters = player.videoDecoderCounters?.apply { ensureUpdated() }
					listener.result(
						clip = clip,
						firstFrameMs = firstFrameMs,
						renderedFrames = counters?.renderedOutputBufferCount ?: 0,
						droppedFrames = counters?.droppedBufferCount ?: 0,
						error = listener.lastError?.errorCodeName,
					)
				}
			}
		} finally {
			player.release()
			// Let the decoder be freed before the next clip asks for one
			delay(1.seconds)
		}
	}

	private class Listener : AnalyticsListener {
		val firstFrameOrError = CompletableDeferred<Any>()
		val error = CompletableDeferred<PlaybackException>()
		var decoder: String? = null
		var lastError: PlaybackException? = null
		var rebuffers = 0
		private var ready = false

		override fun onVideoDecoderInitialized(
			eventTime: AnalyticsListener.EventTime,
			decoderName: String,
			initializedTimestampMs: Long,
			initializationDurationMs: Long,
		) {
			decoder = decoderName
		}

		override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
			firstFrameOrError.complete(Unit)
		}

		override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
			if (state == Player.STATE_READY) ready = true
			else if (state == Player.STATE_BUFFERING && ready) rebuffers++
		}

		override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
			lastError = error
			this.error.complete(error)
			firstFrameOrError.complete(error)
		}

		fun result(
			clip: BenchmarkClip,
			firstFrameMs: Long? = null,
			renderedFrames: Int = 0,
			droppedFrames: Int = 0,
			error: String? = null,
		) = DecoderResult(
			clip = clip.label,
			decoder = decoder,
			hardware = decoder?.let(::isHardwareDecoder),
			firstFrameMs = firstFrameMs,
			renderedFrames = renderedFrames,
			droppedFrames = droppedFrames,
			rebuffers = rebuffers,
			error = error,
		)
	}

	companion object {
		const val DEFAULT_PLAY_SECONDS = 20

		private fun isHardwareDecoder(name: String): Boolean? = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
			.firstOrNull { it.name == name }
			?.isHardwareAccelerated
	}
}
