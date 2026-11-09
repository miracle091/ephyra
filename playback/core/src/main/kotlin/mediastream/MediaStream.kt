package org.jellyfin.playback.core.mediastream

import org.jellyfin.playback.core.queue.QueueEntry

interface MediaStream {
	val identifier: String
	val conversionMethod: MediaConversionMethod
	val container: MediaStreamContainer
	val tracks: Collection<MediaStreamTrack>
}

data class BasicMediaStream(
	override val identifier: String,
	override val conversionMethod: MediaConversionMethod,
	override val container: MediaStreamContainer,
	override val tracks: Collection<MediaStreamTrack>,
) : MediaStream {
	fun toPlayableMediaStream(
		queueEntry: QueueEntry,
		url: String,
	) = PlayableMediaStream(
		identifier = identifier,
		conversionMethod = conversionMethod,
		container = container,
		tracks = tracks,
		queueEntry = queueEntry,
		url = url,
	)
}

data class PlayableMediaStream(
	override val identifier: String,
	override val conversionMethod: MediaConversionMethod,
	override val container: MediaStreamContainer,
	override val tracks: Collection<MediaStreamTrack>,
	val queueEntry: QueueEntry,
	val url: String,
	/**
	 * Ephyra: index (see [MediaStreamAudioTrack.index]) of the audio track the server chose for this stream, or null when unknown.
	 * Only meaningful for a stream that is not converted: a remuxed or transcoded stream carries just the requested track.
	 */
	val defaultAudioStreamIndex: Int? = null,
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
	/** Ephyra: index of the track in the source (Jellyfin media stream index), or null when unknown. */
	val index: Int? = null,
	/** Ephyra: whether the track lives in a separate file, so it is not one of the container's own tracks. */
	val isExternal: Boolean = false,
) : MediaStreamTrack

data class MediaStreamVideoTrack(
	override val codec: String,
	val bitrate: Int,
	val width: Int,
	val height: Int,
	val videoRange: String?,
) : MediaStreamTrack

// TODO: Add subtitle track
