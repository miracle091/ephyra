package org.jellyfin.playback.media3.exoplayer.mapping

import org.jellyfin.playback.core.mediastream.MediaStreamSubtitleTrack
import org.jellyfin.playback.core.mediastream.MediaStreamTrack

private const val EXTERNAL_SUBTITLE_PREFIX = "JF_EXTERNAL:"

/** Ephyra: the id of a subtitle file loaded beside the stream, carrying its Jellyfin media stream index. */
fun externalSubtitleId(streamIndex: Int) = "$EXTERNAL_SUBTITLE_PREFIX$streamIndex"

/** The Jellyfin media stream index in the format id of a subtitle file loaded beside the stream, or null for the stream's own tracks. */
fun externalSubtitleIndex(formatId: String?): Int? = formatId?.substringAfter(EXTERNAL_SUBTITLE_PREFIX, "")?.toIntOrNull()

/** The MIME type for a subtitle [codec], or the one of the file extension in [url] when the codec is not known. */
fun subtitleMimeType(codec: String, url: String?): String {
	val urlExtension = url?.substringBefore('?')?.substringAfterLast('/')?.substringAfterLast('.', "")?.ifBlank { null }
	return urlExtension?.let { getFfmpegSubtitleMimeType(it, "") }?.ifBlank { null }
		?: getFfmpegSubtitleMimeType(codec, "").ifBlank { null }
		?: urlExtension
		?: codec
}

/** Ephyra: what the player should show for the server's subtitle choice. */
sealed interface SubtitleSelection {
	/** No subtitle. */
	data object Off : SubtitleSelection

	/** The text track group at [position] among the player's text track groups. */
	data class Group(val position: Int) : SubtitleSelection

	/** The choice can't be mapped to a player track: the player keeps its own. */
	data object Keep : SubtitleSelection
}

/**
 * Ephyra: maps the server's subtitle choice ([wantedIndex], a Jellyfin media stream index, or null for none) to the player's text
 * track groups, given for each group the stream index of its external file ([groupExternalIndexes], null for the stream's own).
 *
 * An external file is found by its id. On direct play the container's own subtitles come in the same order in both lists, so the
 * position among the non-external tracks gives the group, as for audio; when the counts differ the order can't be trusted. A
 * converted stream carries at most the chosen subtitle (in the HLS manifest), or none when it is burnt into the picture.
 */
fun findSubtitleSelection(
	tracks: Collection<MediaStreamTrack>,
	wantedIndex: Int?,
	groupExternalIndexes: List<Int?>,
	directPlay: Boolean,
): SubtitleSelection {
	if (wantedIndex == null) return SubtitleSelection.Off
	val subtitles = tracks.filterIsInstance<MediaStreamSubtitleTrack>()
	val wanted = subtitles.firstOrNull { it.index == wantedIndex } ?: return SubtitleSelection.Keep

	if (wanted.externalUrl != null) {
		return groupExternalIndexes.indexOf(wantedIndex).takeIf { it >= 0 }?.let(SubtitleSelection::Group) ?: SubtitleSelection.Keep
	}

	val ownGroups = groupExternalIndexes.indices.filter { groupExternalIndexes[it] == null }
	if (!directPlay) return ownGroups.singleOrNull()?.let(SubtitleSelection::Group) ?: SubtitleSelection.Keep

	val embeddedIndexes = subtitles.filterNot { it.isExternal }.map { it.index }.sorted()
	if (embeddedIndexes.size != ownGroups.size) return SubtitleSelection.Keep
	val ordinal = embeddedIndexes.indexOf(wantedIndex)
	return if (ordinal >= 0) SubtitleSelection.Group(ownGroups[ordinal]) else SubtitleSelection.Keep
}
