package org.jellyfin.androidtv.ui.itemdetail

import org.jellyfin.sdk.model.api.BaseItemDto
import java.time.LocalDateTime

/**
 * Ephyra: whether the programme shown for a channel has ended by [now] (both wall-clock time), so the details screen loads the one on air
 * instead of keeping the old title and times, for example after watching the channel past the programme's end.
 */
fun hasProgramEnded(program: BaseItemDto?, now: LocalDateTime): Boolean = program?.endDate?.isBefore(now) == true
