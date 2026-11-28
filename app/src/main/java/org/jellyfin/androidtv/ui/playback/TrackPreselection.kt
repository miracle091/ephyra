package org.jellyfin.androidtv.ui.playback

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import java.util.UUID

/** Velella: subtitle index that means "no subtitles", as the server reads it (shared with the new player). */
const val SUBTITLES_OFF = org.jellyfin.playback.jellyfin.SUBTITLES_OFF

/**
 * Velella: the audio and subtitle tracks picked on the details screen for one item, used once when that item starts playing.
 * Null means "not picked": the player chooses as usual.
 */
data class TrackChoice(
	val itemId: UUID,
	val mediaSourceId: String?,
	val audioIndex: Int? = null,
	val subtitleIndex: Int? = null,
)

/** Velella: holds the last [TrackChoice] made on the details screen (one item at a time). */
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

/** Velella: the media source the legacy player picks for [this] item: the one with the item's id, or the first. */
val BaseItemDto.playedMediaSource: MediaSourceInfo?
	get() = mediaSources?.let { sources -> sources.firstOrNull { it.id?.toUUIDOrNull() == id } ?: sources.firstOrNull() }
