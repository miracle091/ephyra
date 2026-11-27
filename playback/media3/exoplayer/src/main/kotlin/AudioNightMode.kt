package org.jellyfin.playback.media3.exoplayer

import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import androidx.media3.common.C
import timber.log.Timber

private const val LIMITER_ATTACK_MS = 30f
private const val LIMITER_RELEASE_MS = 300f
private const val LIMITER_RATIO = 10f
private const val LIMITER_THRESHOLD_DB = -24f
private const val LIMITER_POST_GAIN_DB = 3f
private const val EQ_BAND_COUNT = 5
private const val EQ_GAIN_LOW = 0.02f
private const val EQ_GAIN_HIGH = 0.03f

// Night mode curve: a gentle boost of the lower-mid bands, per band
private val NIGHTMODE_EQ_GAINS = floatArrayOf(0f, EQ_GAIN_LOW, EQ_GAIN_HIGH, EQ_GAIN_LOW, 0f)

/**
 * Velella: audio night mode (a limiter that narrows the loudness range, plus a light mid boost) on one player's audio session,
 * shared by both players. Released with the player, so no effect outlives its playback.
 */
class AudioNightMode internal constructor(private val createEffect: (audioSessionId: Int) -> AudioEffect) {
	constructor() : this(::createNightModeEffect)

	private var effect: AudioEffect? = null
	private var effectSessionId = C.AUDIO_SESSION_ID_UNSET

	/** Applies night mode to [audioSessionId]; nothing to do when it already runs there. */
	fun apply(audioSessionId: Int) {
		if (effect != null && effectSessionId == audioSessionId) return
		release()
		// Session 0 is the device's whole output mix, not this player's audio
		if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

		Timber.i("Enabling audio night mode for session $audioSessionId")
		effect = runCatching { createEffect(audioSessionId) }
			.onFailure { Timber.w(it, "Audio night mode is not available for session $audioSessionId") }
			.getOrNull()
		effectSessionId = audioSessionId
	}

	fun release() {
		effect?.release()
		effect = null
		effectSessionId = C.AUDIO_SESSION_ID_UNSET
	}
}

private fun createNightModeEffect(audioSessionId: Int): AudioEffect = DynamicsProcessing(0, audioSessionId, null).apply {
	setLimiterAllChannelsTo(
		DynamicsProcessing.Limiter(
			true,
			true,
			1,
			LIMITER_ATTACK_MS,
			LIMITER_RELEASE_MS,
			LIMITER_RATIO,
			LIMITER_THRESHOLD_DB,
			LIMITER_POST_GAIN_DB
		)
	)

	setPreEqAllChannelsTo(DynamicsProcessing.Eq(true, true, EQ_BAND_COUNT).apply {
		NIGHTMODE_EQ_GAINS.forEachIndexed { band, gain -> getBand(band).gain = gain }
	})

	enabled = true
}
