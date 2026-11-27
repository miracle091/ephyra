package org.jellyfin.androidtv.ui.playback

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaStream

class VideoQueueManager {
	private var _currentVideoQueue: List<BaseItemDto> = emptyList()
	private var _currentMediaPosition = -1
	private var _lastPlayedAudioLanguageIsoCode: String? = null
	private var _lastPlayedSubtitleLanguageIsoCode: String? = null
	private var _lastPlayedAudioTrack: TrackInfo? = null
	private var _lastPlayedSubtitleTrack: TrackInfo? = null

	fun setCurrentVideoQueue(items: List<BaseItemDto>?) {
		if (items.isNullOrEmpty()) return clearVideoQueue()

		_currentVideoQueue = items.toMutableList()
		_currentMediaPosition = 0
	}

	fun getCurrentVideoQueue(): List<BaseItemDto> = _currentVideoQueue

	fun setCurrentMediaPosition(currentMediaPosition: Int) {
		if (currentMediaPosition !in 0.._currentVideoQueue.size) return

		_currentMediaPosition = currentMediaPosition
	}

	fun getCurrentMediaPosition() = _currentMediaPosition

	fun getLastPlayedAudioLanguageIsoCode(): String? {
		return _lastPlayedAudioLanguageIsoCode
	}

	fun getLastPlayedSubtitleLanguageIsoCode(): String? {
		return _lastPlayedSubtitleLanguageIsoCode
	}

	fun setLastPlayedSubtitleLanguageIsoCode(isoCode: String?) {
		_lastPlayedSubtitleLanguageIsoCode = isoCode
		if (_lastPlayedSubtitleTrack?.language != isoCode) _lastPlayedSubtitleTrack = null
	}

	/** Velella: the exact audio track last played in this queue, to pick between tracks of the same language. */
	fun getLastPlayedAudioTrack(): TrackInfo? = _lastPlayedAudioTrack

	/** Velella: the exact subtitle track last played in this queue, to pick between tracks of the same language. */
	fun getLastPlayedSubtitleTrack(): TrackInfo? = _lastPlayedSubtitleTrack

	/** Velella: remembers [stream] (language and exact track) as the audio track to carry to the next episodes. */
	fun setLastPlayedAudioTrack(stream: MediaStream) {
		val language = stream.language ?: return
		_lastPlayedAudioLanguageIsoCode = language
		_lastPlayedAudioTrack = stream.toTrackInfo()
	}

	/** Velella: remembers [stream] (language and exact track) as the subtitle track to carry to the next episodes. */
	fun setLastPlayedSubtitleTrack(stream: MediaStream) {
		_lastPlayedSubtitleLanguageIsoCode = stream.language
		_lastPlayedSubtitleTrack = stream.toTrackInfo().takeIf { stream.language != null }
	}

	fun clearVideoQueue() {
		_currentVideoQueue = emptyList()
		_currentMediaPosition = -1
		_lastPlayedAudioLanguageIsoCode = null
		_lastPlayedSubtitleLanguageIsoCode = null
		_lastPlayedAudioTrack = null
		_lastPlayedSubtitleTrack = null
	}
}
