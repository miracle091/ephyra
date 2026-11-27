package org.jellyfin.androidtv.ui.playback

import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType

/**
 * Velella: what identifies an audio or subtitle track from one episode to the next. [index] is kept for the current file only:
 * matching ignores it, since it changes from file to file.
 */
data class TrackInfo(
	val index: Int,
	val language: String?,
	val title: String?,
	val codec: String?,
	val isForced: Boolean,
)

fun MediaStream.toTrackInfo() = TrackInfo(index, language, title, codec, isForced)

private const val FORCED_SCORE = 4
private const val TITLE_SCORE = 2
private const val CODEC_SCORE = 1

/**
 * Velella: the index of the candidate that best matches [wanted], or null when no candidate has its language.
 * The language decides first; among the tracks in that language the forced flag counts most, then the title, then the codec.
 * A tie goes to [preferredIndex] (the track already requested), then to the first track.
 */
fun matchTrack(candidates: List<TrackInfo>, wanted: TrackInfo, preferredIndex: Int? = null): Int? {
	val sameLanguage = candidates.filter { it.language.equals(wanted.language, ignoreCase = true) }
	if (sameLanguage.isEmpty()) return null

	fun score(track: TrackInfo): Int {
		var score = 0
		if (track.isForced == wanted.isForced) score += FORCED_SCORE
		if (!wanted.title.isNullOrBlank() && track.title.equals(wanted.title, ignoreCase = true)) score += TITLE_SCORE
		if (!wanted.codec.isNullOrBlank() && track.codec.equals(wanted.codec, ignoreCase = true)) score += CODEC_SCORE
		return score
	}

	val best = sameLanguage.maxOf(::score)
	val top = sameLanguage.filter { score(it) == best }
	return (top.firstOrNull { it.index == preferredIndex } ?: top.first()).index
}

/**
 * Velella: the audio track of [streams] that matches the last audio track played in the queue, or null when there is none
 * or nothing has that language.
 */
fun preferredAudioIndex(queue: VideoQueueManager, streams: List<MediaStream>?): Int? {
	val language = queue.getLastPlayedAudioLanguageIsoCode().takeUnless { it.isNullOrEmpty() } ?: return null
	val wanted = queue.getLastPlayedAudioTrack()?.takeIf { it.language == language }
		?: TrackInfo(index = -1, language = language, title = null, codec = null, isForced = false)
	val candidates = streams.orEmpty().filter { it.type == MediaStreamType.AUDIO }.map { it.toTrackInfo() }
	return matchTrack(candidates, wanted)
}

/**
 * Velella: the subtitle track of [streams] that matches the last subtitle track played in the queue, or null when there is none
 * or nothing has that language. [requestedIndex] wins ties.
 */
fun preferredSubtitleIndex(queue: VideoQueueManager, streams: List<MediaStream>?, requestedIndex: Int?): Int? {
	val language = queue.getLastPlayedSubtitleLanguageIsoCode().takeUnless { it.isNullOrEmpty() } ?: return null
	val wanted = queue.getLastPlayedSubtitleTrack()?.takeIf { it.language == language }
		?: TrackInfo(index = -1, language = language, title = null, codec = null, isForced = false)
	val candidates = streams.orEmpty().filter { it.type == MediaStreamType.SUBTITLE }.map { it.toTrackInfo() }
	return matchTrack(candidates, wanted, requestedIndex)
}
