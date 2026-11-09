package org.jellyfin.playback.media3.exoplayer.shield

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ForwardingRenderer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * Ephyra: with "prefer FFmpeg" the extension audio renderer is placed before MediaCodecAudioRenderer, and it reports every Dolby and DTS
 * format as handled, so the track selector picks it and the Shield decodes to PCM instead of sending the bitstream to the receiver.
 * Here the FFmpeg renderer is wrapped so it steps aside for any format the audio sink can play directly as passthrough;
 * MediaCodecAudioRenderer (next in line) then wins. The sink is the one the
 * player uses and is asked at selection time, so HDMI capability changes are followed.
 */
@OptIn(UnstableApi::class)
open class BitstreamFirstRenderersFactory(
	context: Context,
	private val ffmpegYieldsToPassthrough: Boolean,
) : DefaultRenderersFactory(context) {
	companion object {
		private const val FFMPEG_AUDIO_RENDERER = "androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer"

		/**
		 * True when FFmpeg must not take [sampleMimeType]: it is encoded (not PCM) and the audio sink can play it as bitstream.
		 * [sinkFormatSupport] is the result of [AudioSink.getFormatSupport].
		 */
		fun shouldFfmpegStepAside(sampleMimeType: String?, sinkFormatSupport: Int): Boolean =
			sampleMimeType != null &&
				sampleMimeType != MimeTypes.AUDIO_RAW &&
				sinkFormatSupport == AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY
	}

	override fun buildAudioRenderers(
		context: Context,
		extensionRendererMode: Int,
		mediaCodecSelector: MediaCodecSelector,
		enableDecoderFallback: Boolean,
		audioSink: AudioSink,
		eventHandler: Handler,
		eventListener: AudioRendererEventListener,
		renderers: ArrayList<Renderer>,
	) {
		super.buildAudioRenderers(
			context,
			extensionRendererMode,
			mediaCodecSelector,
			enableDecoderFallback,
			audioSink,
			eventHandler,
			eventListener,
			renderers,
		)

		if (!ffmpegYieldsToPassthrough) return
		for (index in renderers.indices) {
			val renderer = renderers[index]
			if (renderer.javaClass.name == FFMPEG_AUDIO_RENDERER) renderers[index] = PassthroughYieldingRenderer(renderer, audioSink)
		}
	}

	/** Reports [Format]s the sink can pass through as unsupported, and forwards everything else to the wrapped renderer. */
	private class PassthroughYieldingRenderer(
		renderer: Renderer,
		private val audioSink: AudioSink,
	) : ForwardingRenderer(renderer) {
		// ForwardingRenderer hands out the wrapped renderer's own capabilities, which would bypass the check
		private val capabilities = object : RendererCapabilities {
			private val delegate = renderer.capabilities

			override fun getName(): String = delegate.name

			override fun getTrackType(): Int = delegate.trackType

			override fun supportsFormat(format: Format): Int =
				if (shouldFfmpegStepAside(format.sampleMimeType, audioSink.getFormatSupport(format))) {
					RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE)
				} else {
					delegate.supportsFormat(format)
				}

			override fun supportsMixedMimeTypeAdaptation(): Int = delegate.supportsMixedMimeTypeAdaptation()

			override fun setListener(listener: RendererCapabilities.Listener) = delegate.setListener(listener)

			override fun clearListener() = delegate.clearListener()
		}

		override fun getCapabilities(): RendererCapabilities = capabilities
	}
}
