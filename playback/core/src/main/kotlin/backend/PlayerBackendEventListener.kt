package org.jellyfin.playback.core.backend

import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackError
import org.jellyfin.playback.core.model.SelectedTracks

abstract class PlayerBackendEventListener {
	open fun onPlayStateChange(state: PlayState) = Unit

	/** Ephyra: why playback failed; sent just before the change to [PlayState.ERROR]. */
	open fun onPlayError(error: PlaybackError) = Unit
	open fun onVideoSizeChange(width: Int, height: Int) = Unit
	open fun onMediaStreamEnd(mediaStream: PlayableMediaStream) = Unit

	/** Ephyra: the player now shows these tracks (the server's choice once a stream starts, or a track picked later). */
	open fun onSelectedTracksChange(tracks: SelectedTracks) = Unit
}
