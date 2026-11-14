package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.koin.android.ext.android.inject
import timber.log.Timber

// Subtitle switching steps used by [setSubtitleIndex].

private fun findSubtitleStream(mediaSource: MediaSourceInfo, index: Int): MediaStream? =
	mediaSource.mediaStreams?.firstOrNull { it.type == MediaStreamType.SUBTITLE && it.index == index }

/** Save subtitle language preference for restoration after NextUp screen */
internal fun PlaybackController.saveLastPlayedSubtitle(index: Int) {
	val videoQueueManager by fragment.inject<VideoQueueManager>()
	if (index == -1) {
		// Use empty string to indicate "subtitles explicitly disabled" vs null meaning "no preference"
		videoQueueManager.setLastPlayedSubtitleLanguageIsoCode("")
	} else {
		val stream = findSubtitleStream(currentMediaSource, index)
		// Ephyra: remember the exact track too, so the next episode picks it among the tracks of that language
		if (stream != null) {
			videoQueueManager.setLastPlayedSubtitleTrack(stream)
		} else {
			videoQueueManager.setLastPlayedSubtitleLanguageIsoCode(null)
		}
	}
}

internal fun PlaybackController.disableSubtitles() {
	mCurrentOptions.subtitleStreamIndex = -1

	if (burningSubs) {
		Timber.i("Disabling subtitle baking")

		keepAudioForRestart()
		stop()
		burningSubs = false
		play(mCurrentPosition, -1)
	} else {
		disableDefaultSubtitles()
	}
}

@OptIn(UnstableApi::class)
internal fun PlaybackController.enableSubtitleStream(index: Int) {
	val mediaSource = currentMediaSource
	val stream = findSubtitleStream(mediaSource, index)
	if (stream == null) {
		Timber.w("Failed to find correct media stream")
		return setSubtitleIndex(-1)
	}

	when {
		stream.deliveryMethod == SubtitleDeliveryMethod.ENCODE || shouldBurnInSubtitles(currentStreamInfo.playMethod) -> {
			Timber.i("Restarting playback for subtitle baking")

			keepAudioForRestart()
			stop()
			burningSubs = true
			mCurrentOptions.subtitleStreamIndex = index
			play(mCurrentPosition, index)
		}

		stream.deliveryMethod == SubtitleDeliveryMethod.EXTERNAL ||
			stream.deliveryMethod == SubtitleDeliveryMethod.EMBED ||
			stream.deliveryMethod == SubtitleDeliveryMethod.HLS -> selectSubtitleGroup(mediaSource, stream, index)

		stream.deliveryMethod == SubtitleDeliveryMethod.DROP || stream.deliveryMethod == null -> {
			Timber.i("Dropping subtitles")
			setSubtitleIndex(-1)
		}
	}
}

/** Whether this is a group with a single format (the subtitles) that is added by us as external subtitle [index]. */
@OptIn(UnstableApi::class)
private fun Tracks.Group.isExternalSubtitleGroup(index: Int): Boolean =
	length == 1 && getTrackFormat(0).id?.endsWith(":JF_EXTERNAL:$index") == true

/** The index of [stream] among the subtitle tracks ExoPlayer exposes for its delivery method, or null if it is not there. */
private fun findLocalSubtitleIndex(mediaSource: MediaSourceInfo, stream: MediaStream): Int? =
	mediaSource.mediaStreams.orEmpty()
		.filter { it.type == MediaStreamType.SUBTITLE }
		.filter {
			if (stream.deliveryMethod == SubtitleDeliveryMethod.EMBED) {
				// ExoPlayer exposes every subtitle track of the container, including the ones the server
				// would burn in (ASS/SSA without libass), so they must be counted to match its track groups
				!it.isExternal
			} else {
				it.deliveryMethod == SubtitleDeliveryMethod.EMBED || it.deliveryMethod == SubtitleDeliveryMethod.HLS
			}
		}
		.indexOf(stream)
		.takeIf { it != -1 }

@OptIn(UnstableApi::class)
private fun PlaybackController.selectSubtitleGroup(mediaSource: MediaSourceInfo, stream: MediaStream, index: Int) {
	// External subtitles need to be resolved differently
	val group = if (stream.deliveryMethod == SubtitleDeliveryMethod.EXTERNAL) {
		mVideoManager.mExoPlayer.currentTracks.groups.firstOrNull { group ->
			// Because ExoPlayer uses a MergingMediaSource, each external subtitle format id is prefixed with its source index
			// (normally starting at 1, increasing for each external subttitle). So we only check the end of the id
			group.isExternalSubtitleGroup(index)
		}
	} else {
		// The server does not send a reliable index in all cases, so calculate it manually
		val localIndex = findLocalSubtitleIndex(mediaSource, stream)

		if (localIndex == null) {
			Timber.w("Failed to find local subtitle index")
			return setSubtitleIndex(-1)
		}

		mVideoManager.mExoPlayer.currentTracks.groups
			.filter { it.type == C.TRACK_TYPE_TEXT }
			.filterNot { it.isExternalSubtitleGroup(index) }
			.getOrNull(localIndex)
	}?.mediaTrackGroup

	if (group == null) {
		Timber.w("Failed to find correct subtitle group for method ${stream.deliveryMethod}")
		return setSubtitleIndex(-1)
	}

	Timber.i("Enabling subtitle group $index via method ${stream.deliveryMethod}")
	mCurrentOptions.subtitleStreamIndex = index
	with(mVideoManager.mExoPlayer.trackSelector!!) {
		parameters = parameters.buildUpon()
			.clearOverridesOfType(C.TRACK_TYPE_TEXT)
			.addOverride(TrackSelectionOverride(group, 0))
			.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
			.build()
	}
}
