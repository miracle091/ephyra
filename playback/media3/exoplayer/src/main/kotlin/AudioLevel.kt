package org.jellyfin.playback.media3.exoplayer

import androidx.media3.common.C
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Velella: the loudness of the audio being played, from 0 to 1, for visuals that follow the music (the music player's backdrop).
 * It reads the decoded PCM through a [TeeAudioProcessor], so it needs no permission; it stays at 0 for passthrough or offloaded
 * audio, which never reaches the processors.
 */
object AudioLevel {
	private val _level = MutableStateFlow(0f)
	val level: StateFlow<Float> = _level.asStateFlow()

	internal fun update(value: Float) {
		_level.value = value
	}
}

private const val PCM_16BIT_MAX = 32768f

// Every 4th sample is plenty for a loudness estimate and keeps the work on the playback thread small
private const val SAMPLE_STEP = 4

internal class AudioLevelSink : TeeAudioProcessor.AudioBufferSink {
	private var encoding = C.ENCODING_INVALID

	override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
		this.encoding = encoding
		AudioLevel.update(0f)
	}

	override fun handleBuffer(buffer: ByteBuffer) {
		val data = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
		var sum = 0f
		var count = 0
		when (encoding) {
			C.ENCODING_PCM_16BIT -> {
				val samples = data.asShortBuffer()
				var i = 0
				while (i < samples.limit()) {
					val value = samples.get(i) / PCM_16BIT_MAX
					sum += value * value
					count++
					i += SAMPLE_STEP
				}
			}

			C.ENCODING_PCM_FLOAT -> {
				val samples = data.asFloatBuffer()
				var i = 0
				while (i < samples.limit()) {
					val value = samples.get(i)
					sum += value * value
					count++
					i += SAMPLE_STEP
				}
			}

			else -> return
		}

		if (count > 0) AudioLevel.update(sqrt(sum / count).coerceIn(0f, 1f))
	}
}
