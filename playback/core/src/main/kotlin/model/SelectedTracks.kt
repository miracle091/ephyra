package org.jellyfin.playback.core.model

/**
 * Velella: the audio and subtitle tracks the player shows, by their index in the source (Jellyfin media stream index); null when
 * unknown for audio, or when no subtitle is shown.
 */
data class SelectedTracks(
	val audioIndex: Int? = null,
	val subtitleIndex: Int? = null,
) {
	companion object {
		val NONE = SelectedTracks()
	}
}
