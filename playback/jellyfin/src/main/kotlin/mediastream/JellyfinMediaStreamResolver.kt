package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStreamResolver
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.allowsDirectPlay
import org.jellyfin.playback.core.queue.allowsDirectStream
import org.jellyfin.playback.core.queue.startPosition
import org.jellyfin.playback.core.queue.streamFallback
import org.jellyfin.playback.jellyfin.queue.audioStreamIndex
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.mediaSourceId
import org.jellyfin.playback.jellyfin.queue.subtitleStreamIndex
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.videoApi
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.extensions.inWholeTicks

/**
 * Velella: the user's settings and client for the PlaybackInfo requests, read for each request so a changed setting applies from
 * the next stream.
 */
class StreamRequestOptions(
	/** The "burn in subtitles when transcoding" setting. */
	val alwaysBurnInSubtitles: () -> Boolean = { false },
	/** The client for PlaybackInfo requests, which may wait longer than the default while the server tunes a channel; null for the app's. */
	val playbackInfoApi: (() -> ApiClient)? = null,
	/** The "direct play live TV" setting; off sends channels through the server's transcode. */
	val liveTvDirectPlay: () -> Boolean = { true },
)

class JellyfinMediaStreamResolver(
	private val api: ApiClient,
	private val deviceProfileBuilder: () -> DeviceProfile,
	private val options: StreamRequestOptions = StreamRequestOptions(),
) : MediaStreamResolver {
	companion object {
		private val supportedMediaTypes = arrayOf(MediaType.VIDEO, MediaType.AUDIO)
	}

	override suspend fun getStream(queueEntry: QueueEntry): PlayableMediaStream? {
		val baseItem = queueEntry.baseItem
		if (baseItem == null || !supportedMediaTypes.contains(baseItem.mediaType)) return null

		val mediaInfo = requestMediaInfo(baseItem, queueEntry)
		return when {
			// Direct play video
			mediaInfo.mediaSource.canDirectPlay && baseItem.mediaType == MediaType.VIDEO -> mediaInfo.toStream(
				queueEntry = queueEntry,
				conversionMethod = MediaConversionMethod.None,
				// Velella: a remote HTTP source plays from where it is, as in the legacy player
				url = mediaInfo.mediaSource.remoteDirectUrl ?: api.videoApi.getVideoStreamUrl(
					itemId = baseItem.id,
					container = mediaInfo.mediaSource.container,
					mediaSourceId = mediaInfo.mediaSource.id,
					static = true,
					tag = mediaInfo.mediaSource.eTag,
					liveStreamId = mediaInfo.mediaSource.liveStreamId,
				)
			)

			// Direct play audio
			mediaInfo.mediaSource.canDirectPlay && baseItem.mediaType == MediaType.AUDIO -> mediaInfo.toStream(
				queueEntry = queueEntry,
				conversionMethod = MediaConversionMethod.None,
				url = api.audioApi.getAudioStreamUrl(
					itemId = baseItem.id,
					container = mediaInfo.mediaSource.container,
					mediaSourceId = mediaInfo.mediaSource.id,
					static = true,
					tag = mediaInfo.mediaSource.eTag,
					liveStreamId = mediaInfo.mediaSource.liveStreamId,
				)
			)

			// Remux (direct stream)
			mediaInfo.mediaSource.supportsDirectStream && mediaInfo.mediaSource.transcodingUrl != null -> mediaInfo.toStream(
				queueEntry = queueEntry,
				conversionMethod = MediaConversionMethod.Remux,
				url = api.createUrl(requireNotNull(mediaInfo.mediaSource.transcodingUrl), ignorePathParameters = true)
			)

			// Transcode
			mediaInfo.mediaSource.supportsTranscoding && mediaInfo.mediaSource.transcodingUrl != null -> mediaInfo.toStream(
				queueEntry = queueEntry,
				conversionMethod = MediaConversionMethod.Transcode,
				url = api.createUrl(requireNotNull(mediaInfo.mediaSource.transcodingUrl), ignorePathParameters = true)
			)

			// No compatible stream found
			else -> null
		}
	}

	private suspend fun requestMediaInfo(baseItem: BaseItemDto, queueEntry: QueueEntry): MediaInfo {
		// Velella: after playback errors the entry asks for a heavier conversion (retry chain in MediaStreamService)
		val isLive = baseItem.isLiveTvChannel
		val enableDirectPlay = queueEntry.streamFallback.allowsDirectPlay && (!isLive || options.liveTvDirectPlay())
		val mediaSourceId = if (isLive) null else queueEntry.mediaSourceId
		val mediaInfo = getPlaybackInfo(baseItem, queueEntry, mediaSourceId, enableDirectPlay)
		// Velella: Media3 can't read Blu-ray transport streams, and the path is only known from the answer: ask again without direct play
		if (mediaInfo.mediaSource.canDirectPlay && isBluRayTransportStream(mediaInfo.mediaSource.path)) {
			return getPlaybackInfo(baseItem, queueEntry, mediaInfo.mediaSource.id, enableDirectPlay = false)
		}
		return mediaInfo
	}

	private suspend fun getPlaybackInfo(
		item: BaseItemDto,
		queueEntry: QueueEntry,
		mediaSourceId: String?,
		enableDirectPlay: Boolean = true,
	): MediaInfo {
		val profile = deviceProfileBuilder()
		val isLive = item.isLiveTvChannel
		val response by (options.playbackInfoApi?.invoke() ?: api).mediaInfoApi.getPostedPlaybackInfo(
			itemId = item.id,
			data = PlaybackInfoDto(
				mediaSourceId = mediaSourceId,
				// Velella: where playback starts (resume, or a new stream for another track), so a transcode starts there; live starts now
				startTimeTicks = if (isLive) null else queueEntry.startPosition?.inWholeTicks,
				// Velella: the requested audio track; without one the server picks from the user's language preferences
				audioStreamIndex = queueEntry.audioStreamIndex,
				// Velella: the requested subtitle (-1 for none), so the server picks its delivery (file, manifest or burnt in)
				subtitleStreamIndex = queueEntry.subtitleStreamIndex,
				// Velella: the user's "burn in subtitles when transcoding" setting, as in the legacy player
				alwaysBurnInSubtitleWhenTranscoding = options.alwaysBurnInSubtitles(),
				deviceProfile = profile,
				enableDirectPlay = enableDirectPlay,
				enableDirectStream = queueEntry.streamFallback.allowsDirectStream,
				enableTranscoding = true,
				allowVideoStreamCopy = true,
				allowAudioStreamCopy = true,
				// Velella: a live channel's stream is opened by this request, as in the legacy player
				autoOpenLiveStream = isLive,
			)
		)

		if (response.errorCode != null) {
			error("Failed to get media info for item ${item.id} source ${mediaSourceId}: ${response.errorCode}")
		}

		val mediaSource = selectMediaSource(response.mediaSources, mediaSourceId)

		requireNotNull(mediaSource) {
			"Failed to get media info for item ${item.id} source ${mediaSourceId}: media source missing in response"
		}

		return MediaInfo(
			playSessionId = response.playSessionId.orEmpty(),
			mediaSource = mediaSource
		)
	}

	private fun MediaInfo.toStream(
		queueEntry: QueueEntry,
		conversionMethod: MediaConversionMethod,
		url: String,
	): PlayableMediaStream {
		val defaultSubtitleStreamIndex = mediaSource.defaultSubtitleStreamIndex?.takeIf { it >= 0 }
		val tracks = getTracks { path -> api.createUrl(path, ignorePathParameters = true) }
		// Velella: with "burn in when transcoding" the server burns the subtitle in but still lists it as a file: don't load it again
		val burntIn = conversionMethod == MediaConversionMethod.Transcode && options.alwaysBurnInSubtitles()
		return PlayableMediaStream(
			identifier = playSessionId,
			conversionMethod = conversionMethod,
			container = getMediaStreamContainer(),
			tracks = if (burntIn) tracks.withBurntInSubtitle(defaultSubtitleStreamIndex) else tracks,
			queueEntry = queueEntry,
			url = url,
			defaultAudioStreamIndex = mediaSource.defaultAudioStreamIndex,
			mediaSourceId = mediaSource.id,
			defaultSubtitleStreamIndex = defaultSubtitleStreamIndex,
			isLive = queueEntry.baseItem?.isLiveTvChannel == true,
		)
	}
}
