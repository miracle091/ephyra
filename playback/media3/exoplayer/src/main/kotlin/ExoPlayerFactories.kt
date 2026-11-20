package org.jellyfin.playback.media3.exoplayer

import android.app.ActivityManager
import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.getSystemService
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import io.github.peerless2012.ass.media.AssHandler
import io.github.peerless2012.ass.media.factory.AssRenderersFactory
import io.github.peerless2012.ass.media.kt.withAssMkvSupport
import io.github.peerless2012.ass.media.parser.AssSubtitleParserFactory
import org.jellyfin.playback.media3.exoplayer.shield.BitstreamFirstRenderersFactory
import org.jellyfin.playback.media3.exoplayer.shield.ShieldLoadControl
import org.jellyfin.playback.media3.exoplayer.shield.ShieldMediaCodecSelector

/** Ephyra: the media source factory, with libass parsing of ASS/SSA subtitles when [assHandler] is set. */
@OptIn(UnstableApi::class)
internal fun buildMediaSourceFactory(
	context: Context,
	options: ExoPlayerOptions,
	assHandler: AssHandler?,
): DefaultMediaSourceFactory {
	val dataSourceFactory = DefaultDataSource.Factory(
		context,
		options.baseDataSourceFactory,
	)
	val extractorsFactory = DefaultExtractorsFactory().apply {
		val isLowRamDevice = context.getSystemService<ActivityManager>()?.isLowRamDevice == true
		setTsExtractorTimestampSearchBytes(
			when (isLowRamDevice) {
				true -> ExoPlayerBackend.TS_SEARCH_BYTES_LM
				false -> ExoPlayerBackend.TS_SEARCH_BYTES_HM
			}
		)
		setConstantBitrateSeekingEnabled(true)
		setConstantBitrateSeekingAlwaysEnabled(true)
	}

	return if (assHandler != null) {
		val assSubtitleParserFactory = AssSubtitleParserFactory(assHandler)
		val assExtractorsFactory = extractorsFactory.withAssMkvSupport(assSubtitleParserFactory, assHandler)
		DefaultMediaSourceFactory(dataSourceFactory, assExtractorsFactory).apply {
			setSubtitleParserFactory(assSubtitleParserFactory)
		}
	} else DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
}

/**
 * Ephyra: the renderers: the default audio sink plus a tee that measures the loudness for the music player's backdrop
 * (AudioLevel); when FFmpeg is preferred it still steps aside for formats the sink can pass through as bitstream. With
 * [assHandler] set, libass draws the ASS/SSA subtitles.
 */
@OptIn(UnstableApi::class)
internal fun buildRenderersFactory(
	context: Context,
	options: ExoPlayerOptions,
	assHandler: AssHandler?,
): RenderersFactory {
	val renderersFactory = object : BitstreamFirstRenderersFactory(context, options.preferFfmpeg) {
		override fun buildAudioSink(
			context: Context,
			enableFloatOutput: Boolean,
			enableAudioOutputPlaybackParameters: Boolean,
		): AudioSink = DefaultAudioSink.Builder(context)
			.setEnableFloatOutput(enableFloatOutput)
			.setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParameters)
			.setAudioProcessors(arrayOf(TeeAudioProcessor(AudioLevelSink())))
			.build()
	}.apply {
		setMediaCodecSelector(ShieldMediaCodecSelector())
		setEnableDecoderFallback(true)
		setExtensionRendererMode(
			when (options.preferFfmpeg) {
				true -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
				false -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
			}
		)
	}

	return if (assHandler != null) AssRenderersFactory(assHandler, renderersFactory) else renderersFactory
}

/** Ephyra: the Shield's buffers, with the user's durations where set. */
@OptIn(UnstableApi::class)
internal fun buildLoadControl(context: Context, options: ExoPlayerOptions): LoadControl = ShieldLoadControl.build(
	context,
	options.minBufferDuration?.inWholeMilliseconds?.toInt() ?: DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
	options.maxBufferDuration?.inWholeMilliseconds?.toInt() ?: DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
	options.bufferForPlaybackDuration?.inWholeMilliseconds?.toInt() ?: DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
	options.bufferForPlaybackAfterRebufferDuration?.inWholeMilliseconds?.toInt()
		?: DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
)

/** Ephyra: the track selector, with audio offload off. */
@OptIn(UnstableApi::class)
internal fun buildTrackSelector(context: Context): DefaultTrackSelector = DefaultTrackSelector(context).apply {
	setParameters(buildUponParameters().apply {
		setAudioOffloadPreferences(
			TrackSelectionParameters.AudioOffloadPreferences.DEFAULT.buildUpon().apply {
				// Ephyra: a mains-powered TV box gains nothing from DSP offload, and offload is preferred over HDMI bitstream
				// passthrough when the platform offers both (DefaultAudioSink.getFormatSupport), so keep it off
				setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
			}.build()
		)
		setAllowInvalidateSelectionsOnRendererCapabilitiesChange(true)
	})
}
