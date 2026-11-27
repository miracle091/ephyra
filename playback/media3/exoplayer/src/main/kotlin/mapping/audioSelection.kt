package org.jellyfin.playback.media3.exoplayer.mapping

import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamTrack

/**
 * Velella: position, among the audio tracks ExoPlayer exposes for a directly played file, of the audio track with the Jellyfin
 * media stream index [streamIndex]. The container's own audio tracks come in the same order in both lists, so the position among
 * the non-external audio tracks (sorted by stream index) is the position among the player's audio track groups.
 *
 * Returns null when [streamIndex] is not one of the container's audio tracks, or when the player shows a different number of
 * audio track groups ([playerAudioGroupCount]) than the server lists: the order can't be trusted then and the player keeps its choice.
 */
fun findAudioGroupPosition(tracks: Collection<MediaStreamTrack>, streamIndex: Int, playerAudioGroupCount: Int): Int? {
	val containerAudioIndexes = tracks
		.filterIsInstance<MediaStreamAudioTrack>()
		.filterNot { it.isExternal }
		.mapNotNull { it.index }
		.sorted()

	if (containerAudioIndexes.size != playerAudioGroupCount) return null
	return containerAudioIndexes.indexOf(streamIndex).takeIf { it >= 0 }
}
