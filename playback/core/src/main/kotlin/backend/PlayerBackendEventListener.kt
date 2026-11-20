package org.jellyfin.playback.core.backend

import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackError

abstract class PlayerBackendEventListener {
	open fun onPlayStateChange(state: PlayState) = Unit

	/** Ephyra: why playback failed; sent just before the change to [PlayState.ERROR]. */
	open fun onPlayError(error: PlaybackError) = Unit
	open fun onVideoSizeChange(width: Int, height: Int) = Unit
	open fun onMediaStreamEnd(mediaStream: PlayableMediaStream) = Unit
}
