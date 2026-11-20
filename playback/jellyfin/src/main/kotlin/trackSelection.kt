package org.jellyfin.playback.jellyfin

import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.audioStreamIndex
import org.jellyfin.playback.jellyfin.queue.subtitleStreamIndex
import timber.log.Timber

/** Ephyra: subtitle index that means "no subtitles", as the server reads it. */
const val SUBTITLES_OFF = -1

/**
 * Ephyra: switch the current entry to the audio track with the source index [index]: in the player when the stream carries it,
 * otherwise the server builds a new stream with it, played on from the current position. The pick is kept on the entry, so a
 * later new stream (another subtitle) keeps it.
 */
fun PlaybackManager.selectAudioTrack(index: Int) {
	val entry = queue.entry.value ?: return
	entry.audioStreamIndex = index
	// While the entry's stream is still being asked for, the backend still plays the previous entry: nothing to switch there
	if (entry.mediaStream == null) return
	if (backend.selectAudioTrack(index)) return

	Timber.i("Audio track $index needs a new stream")
	reloadStream()
}

/**
 * Ephyra: show the subtitle with the source index [index], or none for null: in the player when the stream allows it, otherwise
 * the server builds a new stream with it (burnt in, or in the manifest), played on from the current position. The pick is kept on
 * the entry, so a later new stream (another audio track) keeps it.
 */
fun PlaybackManager.selectSubtitleTrack(index: Int?) {
	val entry = queue.entry.value ?: return
	entry.subtitleStreamIndex = index ?: SUBTITLES_OFF
	if (entry.mediaStream == null) return
	if (backend.selectSubtitleTrack(index)) return

	Timber.i("Subtitle $index needs a new stream")
	reloadStream()
}
