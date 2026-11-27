package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo

/**
 * Velella: a live TV channel. Its stream is opened by the PlaybackInfo request itself (`autoOpenLiveStream`), it has no start
 * position, and its media source id changes with every opened stream, so the request names none.
 */
val BaseItemDto.isLiveTvChannel: Boolean
	get() = type == BaseItemKind.TV_CHANNEL

/** Velella: protocols Media3 reads directly; the server converts the others (RTSP, UDP, RTMP...) for us. */
private val directPlayProtocols = setOf(MediaProtocol.FILE, MediaProtocol.HTTP)

/**
 * Velella: the media source to play from a PlaybackInfo answer: the requested one, or the first. Live channels, `.strm` files and
 * other remote sources are kept (the server streams or converts them), not only local files as upstream did.
 */
fun selectMediaSource(sources: List<MediaSourceInfo>, mediaSourceId: String?): MediaSourceInfo? =
	sources.firstOrNull { mediaSourceId == null || it.id == mediaSourceId }

/** Velella: whether the source can be played as it is, which also needs a protocol Media3 reads. */
val MediaSourceInfo.canDirectPlay: Boolean
	get() = supportsDirectPlay && protocol in directPlayProtocols

/**
 * Velella: the address of a remote HTTP source (a `.strm` file, a tuner's stream) to play directly from where it is, as the
 * legacy player does; null when the server's static stream is used instead.
 */
val MediaSourceInfo.remoteDirectUrl: String?
	get() = path?.takeIf { isRemote && protocol == MediaProtocol.HTTP && it.isNotBlank() }
