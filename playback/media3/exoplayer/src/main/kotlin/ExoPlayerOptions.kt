package org.jellyfin.playback.media3.exoplayer

import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import kotlin.time.Duration

data class ExoPlayerOptions(
	val preferFfmpeg: Boolean = false,
	val enableDebugLogging: Boolean = false,
	val enableLibass: Boolean = false,
	val baseDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory(),
	val minBufferDuration: Duration? = null,
	val maxBufferDuration: Duration? = null,
	val bufferForPlaybackDuration: Duration? = null,
	val bufferForPlaybackAfterRebufferDuration: Duration? = null,
	/** Ephyra: a `C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_*` value; OFF when the app switches the display mode itself. */
	val videoChangeFrameRateStrategy: Int = C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS,
	/** Ephyra: the user's look for text subtitles; null keeps Media3's default. */
	val subtitleStyle: SubtitleStyle? = null,
	/** Ephyra: narrow the loudness range of the player's audio (decoded audio only; bitstream passthrough is left alone). */
	val audioNightMode: Boolean = false,
)
