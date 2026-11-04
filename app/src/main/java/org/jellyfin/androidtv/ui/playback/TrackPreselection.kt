package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.data.compat.VideoOptions
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.java.KoinJavaComponent
import java.util.UUID

/** Ephyra: subtitle index that means "no subtitles", as the server reads it. */
const val SUBTITLES_OFF = -1

/**
 * Ephyra: the audio and subtitle tracks picked on the details screen for one item, used once when that item starts playing.
 * Null means "not picked": the player chooses as usual.
 */
data class TrackChoice(
	val itemId: UUID,
	val mediaSourceId: String?,
	val audioIndex: Int? = null,
	val subtitleIndex: Int? = null,
)

/** Ephyra: holds the last [TrackChoice] made on the details screen (one item at a time). */
class TrackPreselection {
	private var choice: TrackChoice? = null

	/** The choice for [itemId] and [mediaSourceId], if one was made. */
	fun choiceFor(itemId: UUID, mediaSourceId: String?): TrackChoice? =
		choice?.takeIf { it.itemId == itemId && it.mediaSourceId == mediaSourceId }

	fun pickAudio(itemId: UUID, mediaSourceId: String?, index: Int) {
		choice = (choiceFor(itemId, mediaSourceId) ?: TrackChoice(itemId, mediaSourceId)).copy(audioIndex = index)
	}

	fun pickSubtitle(itemId: UUID, mediaSourceId: String?, index: Int) {
		choice = (choiceFor(itemId, mediaSourceId) ?: TrackChoice(itemId, mediaSourceId)).copy(subtitleIndex = index)
	}

	/** Returns the choice for [itemId] and [mediaSourceId] and forgets it, so it applies to one playback only. */
	fun take(itemId: UUID, mediaSourceId: String?): TrackChoice? = choiceFor(itemId, mediaSourceId)?.also { choice = null }
}

/** Ephyra: the media source the legacy player picks for [this] item: the one with the item's id, or the first. */
val BaseItemDto.playedMediaSource: MediaSourceInfo?
	get() = mediaSources?.let { sources -> sources.firstOrNull { it.id?.toUUIDOrNull() == id } ?: sources.firstOrNull() }

/**
 * Ephyra: puts the tracks picked on the details screen for [item] into the request [options], so the server builds the stream
 * with them from the start. The languages are saved as the "last played" ones too, as a pick in the player would, so the
 * player keeps the picked subtitle once the stream starts and the next episodes follow the same languages.
 */
fun applyTrackPreselection(options: VideoOptions, item: BaseItemDto, mediaSource: MediaSourceInfo?) = applyTrackPreselection(
	options = options,
	item = item,
	mediaSource = mediaSource,
	preselection = KoinJavaComponent.get(TrackPreselection::class.java),
	queue = KoinJavaComponent.get(VideoQueueManager::class.java),
)

internal fun applyTrackPreselection(
	options: VideoOptions,
	item: BaseItemDto,
	mediaSource: MediaSourceInfo?,
	preselection: TrackPreselection,
	queue: VideoQueueManager,
) {
	val choice = preselection.take(item.id, mediaSource?.id) ?: return
	val streams = mediaSource?.mediaStreams.orEmpty()

	choice.audioIndex?.let { index ->
		options.audioStreamIndex = index
		streams.firstOrNull { it.index == index && it.type == MediaStreamType.AUDIO }?.language
			?.let(queue::setLastPlayedAudioLanguageIsoCode)
	}
	choice.subtitleIndex?.let { index ->
		options.subtitleStreamIndex = index
		val language = if (index == SUBTITLES_OFF) "" else streams.firstOrNull { it.index == index }?.language
		queue.setLastPlayedSubtitleLanguageIsoCode(language)
	}
}
