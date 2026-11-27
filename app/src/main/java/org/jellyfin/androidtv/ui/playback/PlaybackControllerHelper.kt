package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.util.sdk.end
import org.jellyfin.androidtv.util.sdk.start
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStreamType
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

fun PlaybackController.getLiveTvChannel(
	id: UUID,
	callback: (channel: BaseItemDto) -> Unit,
) {
	val api by fragment.inject<ApiClient>()

	fragment.lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getChannel(id).content
			}
		}.onSuccess { channel ->
			callback(channel)
		}
	}
}

/** Velella: whether [mediaSource] has an audio stream with this [index]. */
fun hasAudioStream(mediaSource: MediaSourceInfo?, index: Int): Boolean =
	mediaSource?.mediaStreams.orEmpty().any { it.type == MediaStreamType.AUDIO && it.index == index }

@OptIn(UnstableApi::class)
fun PlaybackController.disableDefaultSubtitles() {
	Timber.i("Disabling non-baked subtitles")

	with(mVideoManager.mExoPlayer.trackSelector!!) {
		parameters = parameters.buildUpon()
			.clearOverridesOfType(C.TRACK_TYPE_TEXT)
			.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
			.build()
	}
}

@OptIn(UnstableApi::class)
@JvmOverloads
fun PlaybackController.setSubtitleIndex(index: Int, force: Boolean = false) {
	Timber.i("Switching subtitles from index ${mCurrentOptions.subtitleStreamIndex} to $index")

	// Already using this subtitle index
	if (mCurrentOptions.subtitleStreamIndex == index && !force) return

	saveLastPlayedSubtitle(index)

	when {
		// Disable subtitles
		index == -1 -> disableSubtitles()
		// If we're currently burning subs and want to switch streams we need some special behavior
		// to stop the current baked subs. We can just stop & start with the new subtitle index for that
		burningSubs -> {
			Timber.i("Restarting playback to disable subtitle baking")

			keepAudioForRestart()
			stop()
			burningSubs = false
			mCurrentOptions.subtitleStreamIndex = index
			play(mCurrentPosition, index)
		}

		else -> enableSubtitleStream(index)
	}
}

fun PlaybackController.applyMediaSegments(
	item: BaseItemDto,
	callback: () -> Unit,
) {
	val mediaSegmentRepository by fragment.inject<MediaSegmentRepository>()

	fragment?.clearSkipOverlay()
	fragment.lifecycleScope.launch {
		val mediaSegments = runCatching {
			mediaSegmentRepository.getSegmentsForItem(item)
		}.getOrNull().orEmpty()

		for (mediaSegment in mediaSegments) {
			val action = mediaSegmentRepository.getMediaSegmentAction(mediaSegment)

			when (action) {
				MediaSegmentAction.SKIP -> addSkipAction(mediaSegment)
				MediaSegmentAction.ASK_TO_SKIP -> addAskToSkipAction(mediaSegment)
				MediaSegmentAction.NOTHING -> Unit
			}
		}

		callback()
	}
}

@OptIn(UnstableApi::class)
private fun PlaybackController.addSkipAction(mediaSegment: MediaSegmentDto) {
	mVideoManager.mExoPlayer
		.createMessage { _, _ ->
			// We can't seek directly on the ExoPlayer instance as not all media is seekable
			// the seek function in the PlaybackController checks this and optionally starts a transcode
			// at the requested position
			fragment.lifecycleScope.launch(Dispatchers.Main) {
				seek(mediaSegment.end.inWholeMilliseconds, true)
			}
		}
		// Segments at position 0 will never be hit by ExoPlayer so we need to add a minimum value
		.setPosition(mediaSegment.start.inWholeMilliseconds.coerceAtLeast(1))
		.setDeleteAfterDelivery(false)
		.send()
}

@OptIn(UnstableApi::class)
private fun PlaybackController.addAskToSkipAction(mediaSegment: MediaSegmentDto) {
	mVideoManager.mExoPlayer
		.createMessage { _, _ ->
			fragment?.askToSkip(mediaSegment.end)
		}
		// Segments at position 0 will never be hit by ExoPlayer so we need to add a minimum value
		.setPosition(mediaSegment.start.inWholeMilliseconds.coerceAtLeast(1))
		.setDeleteAfterDelivery(false)
		.send()
}

/** Velella: [channel] with [program] as the programme on air, for the live player's header and record button. */
fun withCurrentProgram(channel: BaseItemDto, program: BaseItemDto): BaseItemDto = channel.copy(currentProgram = program)

/** Velella: looks up the programme that follows the one ending at [after] on a channel, for the live player's "Next:" line. */
fun PlaybackController.loadNextProgram(
	channelId: UUID,
	after: LocalDateTime,
	callback: (next: BaseItemDto?) -> Unit,
) {
	val api by fragment.inject<ApiClient>()

	fragment.lifecycleScope.launch {
		val programs = runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getLiveTvPrograms(
					channelIds = listOf(channelId),
					enableImages = false,
					sortBy = setOf(ItemSortBy.START_DATE),
					minStartDate = after,
					limit = 3,
				).content.items
			}
		}.getOrNull()

		callback(NextProgram.pick(programs, after))
	}
}
