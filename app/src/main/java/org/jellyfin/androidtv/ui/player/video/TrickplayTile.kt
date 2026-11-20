package org.jellyfin.androidtv.ui.player.video

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.TrickplayInfoDto
import kotlin.time.Duration

/**
 * Ephyra: where the trickplay thumbnail for a position sits: the server's tile image [index] (a grid of thumbnails) and the
 * thumbnail's rectangle inside it, in pixels.
 */
data class TrickplayTile(val index: Int, val x: Int, val y: Int, val width: Int, val height: Int)

/** Ephyra: the trickplay thumbnails of [mediaSourceId] in this item, the first resolution the server made, or null for none. */
fun BaseItemDto.trickplayInfo(mediaSourceId: String?): TrickplayInfoDto? =
	trickplay?.get(mediaSourceId ?: return null)?.values?.firstOrNull()

/** Ephyra: the thumbnail of [info] for [position], as the legacy player's seek provider picked it (CustomSeekProvider). */
fun trickplayTile(position: Duration, info: TrickplayInfoDto): TrickplayTile {
	val thumbnail = (position.inWholeMilliseconds.coerceAtLeast(0) / info.interval.coerceAtLeast(1)).toInt()
		.coerceAtMost((info.thumbnailCount - 1).coerceAtLeast(0))
	val perTile = (info.tileWidth * info.tileHeight).coerceAtLeast(1)
	val offset = thumbnail % perTile
	return TrickplayTile(
		index = thumbnail / perTile,
		x = offset % info.tileWidth * info.width,
		y = offset / info.tileWidth * info.height,
		width = info.width,
		height = info.height,
	)
}
