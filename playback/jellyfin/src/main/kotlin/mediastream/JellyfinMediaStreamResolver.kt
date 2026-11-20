package org.jellyfin.playback.jellyfin.mediastream

import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStreamResolver
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.startPosition
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
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.extensions.inWholeTicks

class JellyfinMediaStreamResolver(
	private val api: ApiClient,
	private val deviceProfileBuilder: () -> DeviceProfile,
	// Ephyra: read for each request, so a changed setting applies from the next stream
	private val alwaysBurnInSubtitles: () -> Boolean = { false },
) : MediaStreamResolver {
	companion object {
		private val supportedMediaTypes = arrayOf(MediaType.VIDEO, MediaType.AUDIO)
	}

	override suspend fun getStream(queueEntry: QueueEntry): PlayableMediaStream? {
		val baseItem = queueEntry.baseItem
		if (baseItem == null || !supportedMediaTypes.contains(baseItem.mediaType)) return null

		var mediaInfo = getPlaybackInfo(baseItem, queueEntry, queueEntry.mediaSourceId)
		// Ephyra: Media3 can't read Blu-ray transport streams, and the path is only known from the answer: ask again without direct play
		if (mediaInfo.mediaSource.supportsDirectPlay && isBluRayTransportStream(mediaInfo.mediaSource.path)) {
			mediaInfo = getPlaybackInfo(baseItem, queueEntry, mediaInfo.mediaSource.id, enableDirectPlay = false)
		}

		return when {
			// Direct play video
			mediaInfo.mediaSource.supportsDirectPlay && baseItem.mediaType == MediaType.VIDEO -> mediaInfo.toStream(
				queueEntry = queueEntry,
				conversionMethod = MediaConversionMethod.None,
				url = api.videoApi.getVideoStreamUrl(
					itemId = baseItem.id,
					container = mediaInfo.mediaSource.container,
					mediaSourceId = mediaInfo.mediaSource.id,
					static = true,
					tag = mediaInfo.mediaSource.eTag,
					liveStreamId = mediaInfo.mediaSource.liveStreamId,
				)
			)

			// Direct play audio
			mediaInfo.mediaSource.supportsDirectPlay && baseItem.mediaType == MediaType.AUDIO -> mediaInfo.toStream(
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

	private suspend fun getPlaybackInfo(
		item: BaseItemDto,
		queueEntry: QueueEntry,
		mediaSourceId: String?,
		enableDirectPlay: Boolean = true,
	): MediaInfo {
		val profile = deviceProfileBuilder()
		val response by api.mediaInfoApi.getPostedPlaybackInfo(
			itemId = item.id,
			data = PlaybackInfoDto(
				mediaSourceId = mediaSourceId,
				// Ephyra: where playback starts (resume, or a new stream for another track), so a transcode starts there
				startTimeTicks = queueEntry.startPosition?.inWholeTicks,
				// Ephyra: the requested audio track; without one the server picks from the user's language preferences
				audioStreamIndex = queueEntry.audioStreamIndex,
				// Ephyra: the requested subtitle (-1 for none), so the server picks its delivery (file, manifest or burnt in)
				subtitleStreamIndex = queueEntry.subtitleStreamIndex,
				// Ephyra: the user's "burn in subtitles when transcoding" setting, as in the legacy player
				alwaysBurnInSubtitleWhenTranscoding = alwaysBurnInSubtitles(),
				deviceProfile = profile,
				enableDirectPlay = enableDirectPlay,
				enableDirectStream = true,
				enableTranscoding = true,
				allowVideoStreamCopy = true,
				allowAudioStreamCopy = true,
				autoOpenLiveStream = false,
			)
		)

		if (response.errorCode != null) {
			error("Failed to get media info for item ${item.id} source ${mediaSourceId}: ${response.errorCode}")
		}

		val mediaSource = response.mediaSources
			// Filter out invalid streams (like strm files)
			.filter { it.protocol == MediaProtocol.FILE && !it.isRemote }
			// Select first media source
			.firstOrNull { mediaSourceId == null || it.id == mediaSourceId }

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
		// Ephyra: with "burn in when transcoding" the server burns the subtitle in but still lists it as a file: don't load it again
		val burntIn = conversionMethod == MediaConversionMethod.Transcode && alwaysBurnInSubtitles()
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
		)
	}
}
