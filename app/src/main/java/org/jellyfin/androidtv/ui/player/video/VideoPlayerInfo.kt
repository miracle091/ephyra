package org.jellyfin.androidtv.ui.player.video

import org.jellyfin.androidtv.integration.dream.formatPlaybackTime
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.sdk.model.api.PlayMethod
import java.time.LocalTime
import kotlin.time.Duration

/** Velella: the server's name for how the new player's stream is delivered, for the play info line (ui/playback/PlayInfo.kt). */
val MediaConversionMethod.playMethod: PlayMethod
	get() = when (this) {
		MediaConversionMethod.None -> PlayMethod.DIRECT_PLAY
		MediaConversionMethod.Remux -> PlayMethod.DIRECT_STREAM
		MediaConversionMethod.Transcode -> PlayMethod.TRANSCODE
	}

private val nonHdrRanges = setOf("SDR", "UNKNOWN")

/**
 * Velella: whether [stream] reaches the TV as HDR or Dolby Vision, to dim the controls (ui/playback/HdrControlsDim.kt): the
 * source video is HDR and the server sends it as it is. A transcode is taken as SDR, as the server tone-maps HDR when it converts.
 */
fun isHdrStream(stream: PlayableMediaStream?): Boolean {
	if (stream == null || stream.conversionMethod == MediaConversionMethod.Transcode) return false
	val range = stream.tracks.filterIsInstance<MediaStreamVideoTrack>().firstOrNull()?.videoRange ?: return false
	return range.uppercase() !in nonHdrRanges
}

/**
 * Velella: the audio track playing, for the playback info panel: the one the player reports ([selectedIndex]), else the one the
 * server chose for [stream], else the first. A converted stream lists the source's tracks but carries only the chosen one.
 */
fun playingAudioTrack(stream: PlayableMediaStream, selectedIndex: Int?): MediaStreamAudioTrack? {
	val audioTracks = stream.tracks.filterIsInstance<MediaStreamAudioTrack>()
	val index = selectedIndex ?: stream.defaultAudioStreamIndex
	return audioTracks.firstOrNull { it.index != null && it.index == index } ?: audioTracks.firstOrNull()
}

/** Velella: when the video ends if it plays on from [position] at [speed], for the "Ends at" line under the seek bar. */
fun videoEndsAt(position: Duration, duration: Duration, speed: Float, now: LocalTime): LocalTime {
	val remaining = (duration - position).coerceAtLeast(Duration.ZERO)
	val atSpeed = if (speed > 0f) remaining / speed.toDouble() else remaining
	return now.plusSeconds(atSpeed.inWholeSeconds)
}

/** Velella: the media time left after [position], as "-24:20", on the right under the seek bar. */
fun remainingPlaybackTime(position: Duration, duration: Duration): String =
	"-" + formatPlaybackTime((duration - position).coerceAtLeast(Duration.ZERO).inWholeMilliseconds)

/** Velella: the skip length in whole seconds, drawn inside the rewind and fast forward icons. */
fun skipSecondsLabel(amount: Duration): String = amount.inWholeSeconds.toString()
