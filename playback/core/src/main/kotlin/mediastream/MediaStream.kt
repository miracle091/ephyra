package org.jellyfin.playback.core.mediastream

import org.jellyfin.playback.core.queue.QueueEntry

interface MediaStream {
	val identifier: String
	val conversionMethod: MediaConversionMethod
	val container: MediaStreamContainer
	val tracks: Collection<MediaStreamTrack>
}

data class PlayableMediaStream(
	override val identifier: String,
	override val conversionMethod: MediaConversionMethod,
	override val container: MediaStreamContainer,
	override val tracks: Collection<MediaStreamTrack>,
	val queueEntry: QueueEntry,
	val url: String,
	/**
	 * Velella: index (see [MediaStreamAudioTrack.index]) of the audio track the server chose for this stream, or null when unknown.
	 * Only meaningful for a stream that is not converted: a remuxed or transcoded stream carries just the requested track.
	 */
	val defaultAudioStreamIndex: Int? = null,
	/** Velella: the server's id of the media source (version) this stream plays, for the play session reports. */
	val mediaSourceId: String? = null,
	/** Velella: index of the subtitle track the server chose from the user's settings, or null for none. */
	val defaultSubtitleStreamIndex: Int? = null,
) : MediaStream

data class MediaStreamContainer(
	val format: String,
)

sealed interface MediaStreamTrack {
	val codec: String
}

data class MediaStreamAudioTrack(
	override val codec: String,
	val bitrate: Int,
	val channels: Int,
	val sampleRate: Int,
	/** Velella: index of the track in the source (Jellyfin media stream index), or null when unknown. */
	val index: Int? = null,
	/** Velella: whether the track lives in a separate file, so it is not one of the container's own tracks. */
	val isExternal: Boolean = false,
	/** Velella: the server's display name (language, codec, channels), for the player's audio menu. */
	val title: String? = null,
) : MediaStreamTrack

data class MediaStreamVideoTrack(
	override val codec: String,
	val bitrate: Int,
	val width: Int,
	val height: Int,
	val videoRange: String?,
	/** Velella: the source's real frame rate, used to pick the display mode, or null when unknown. */
	val frameRate: Float? = null,
) : MediaStreamTrack

/** Velella: a subtitle track of the source. */
data class MediaStreamSubtitleTrack(
	override val codec: String,
	/** Index of the track in the source (Jellyfin media stream index). */
	val index: Int,
	val language: String?,
	/** The server's display name (language, codec, flags). */
	val title: String?,
	val isDefault: Boolean,
	val isForced: Boolean,
	/** Whether the track lives in a separate file, so it is not one of the container's own tracks. */
	val isExternal: Boolean,
	/** URL of the subtitle file for the player to load beside the stream, or null when the stream carries the track itself. */
	val externalUrl: String?,
) : MediaStreamTrack
