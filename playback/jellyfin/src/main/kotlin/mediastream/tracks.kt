package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamContainer
import org.jellyfin.playback.core.mediastream.MediaStreamSubtitleTrack
import org.jellyfin.playback.core.mediastream.MediaStreamTrack
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod

fun MediaInfo.getMediaStreamContainer() = MediaStreamContainer(
	format = requireNotNull(mediaSource.container)
)

/** The tracks of this media source; [createUrl] turns the server's relative subtitle delivery path into a full URL. */
fun MediaInfo.getTracks(createUrl: (path: String) -> String) =
	mediaSource.mediaStreams
		.orEmpty()
		.mapNotNull { it.getMediaStreamTrack(createUrl) }

fun MediaStream.getMediaStreamTrack(createUrl: (path: String) -> String) = when (type) {
	MediaStreamType.AUDIO -> getAudioTrack(this)
	MediaStreamType.VIDEO -> getVideoTrack(this)
	MediaStreamType.SUBTITLE -> getSubtitleTrack(this, createUrl)

	// Ignore other track types
	MediaStreamType.EMBEDDED_IMAGE,
	MediaStreamType.DATA,
	MediaStreamType.LYRIC -> null
}

private fun getAudioTrack(stream: MediaStream) = MediaStreamAudioTrack(
	codec = requireNotNull(stream.codec),
	bitrate = stream.bitRate ?: 0,
	channels = stream.channels ?: 1,
	sampleRate = stream.sampleRate ?: 0,
	index = stream.index,
	isExternal = stream.isExternal,
	title = stream.displayTitle ?: stream.title,
)

private fun getVideoTrack(stream: MediaStream) = MediaStreamVideoTrack(
	codec = requireNotNull(stream.codec),
	bitrate = stream.bitRate ?: 0,
	width = stream.width ?: 0,
	height = stream.height ?: 0,
	videoRange = stream.videoRangeType.name,
	frameRate = stream.realFrameRate,
)

// Ephyra: only a track the server hands out as a separate file gets a URL; embedded, HLS and burnt-in ones come with the stream
private fun getSubtitleTrack(stream: MediaStream, createUrl: (path: String) -> String) = MediaStreamSubtitleTrack(
	codec = stream.codec.orEmpty(),
	index = stream.index,
	language = stream.language,
	title = stream.displayTitle ?: stream.title,
	isDefault = stream.isDefault,
	isForced = stream.isForced,
	isExternal = stream.isExternal,
	externalUrl = stream.deliveryUrl
		?.takeIf { stream.deliveryMethod == SubtitleDeliveryMethod.EXTERNAL }
		?.let(createUrl),
)

/** Ephyra: these tracks with the subtitle [index] burnt into the picture, so the player doesn't load its file as well. */
fun List<MediaStreamTrack>.withBurntInSubtitle(index: Int?) = map { track ->
	if (track is MediaStreamSubtitleTrack && track.index == index) track.copy(externalUrl = null) else track
}
