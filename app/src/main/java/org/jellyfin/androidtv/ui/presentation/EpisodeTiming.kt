package org.jellyfin.androidtv.ui.presentation

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.getTimeFormatter
import org.jellyfin.sdk.model.api.BaseItemDto
import java.time.LocalTime
import java.util.Locale

private const val TICKS_PER_MINUTE = 600_000_000L
private const val NANOS_PER_TICK = 100L
private const val MINUTES_PER_HOUR = 60
private const val META_SEPARATOR = " · "

/**
 * Ephyra: the time of day an item that runs [runtimeTicks] ends when started now. An item stopped part way through
 * ([positionTicks] inside the runtime) only counts what is left. Null when the runtime is unknown.
 */
fun endsAt(runtimeTicks: Long?, positionTicks: Long?, now: LocalTime): LocalTime? {
	val runtime = runtimeTicks?.takeIf { it > 0 } ?: return null
	val position = positionTicks?.takeIf { it in 1 until runtime } ?: 0L
	return now.plusNanos((runtime - position) * NANOS_PER_TICK)
}

/** Ephyra: the whole runtime in minutes, at least one; null when the runtime is unknown. */
fun runtimeMinutes(runtimeTicks: Long?): Int? {
	val runtime = runtimeTicks?.takeIf { it > 0 } ?: return null
	return ((runtime + TICKS_PER_MINUTE / 2) / TICKS_PER_MINUTE).toInt().coerceAtLeast(1)
}

/**
 * Ephyra: the meta line of an episode, "47 min · ★ 8.2 · Ends at 22:45"; every part that is unknown is left out and the line is
 * null when none is known.
 */
fun episodeMetaLine(
	runtime: String?,
	communityRating: Float?,
	endsAt: String?,
	locale: Locale = Locale.getDefault(),
): String? = listOfNotNull(
	runtime,
	communityRating?.takeIf { it > 0f }?.let { "★ " + String.format(locale, "%.1f", it) },
	endsAt,
).takeIf { it.isNotEmpty() }?.joinToString(META_SEPARATOR)

/** Ephyra: the title of an episode in the compact list: "1. Name", "1-2. Name" for a double episode, or the name alone. */
fun compactEpisodeTitle(indexNumber: Int?, indexNumberEnd: Int?, name: String?): String {
	val title = name.orEmpty()
	val number = when {
		indexNumber == null -> return title
		indexNumberEnd != null && indexNumberEnd != indexNumber -> "$indexNumber-$indexNumberEnd"
		else -> indexNumber.toString()
	}
	return if (title.isBlank()) number else "$number. $title"
}

/** Ephyra: the meta line of an episode with the strings and the 12/24 hour format of the device. */
fun BaseItemDto.episodeMetaLine(context: Context, now: LocalTime = LocalTime.now()): String? {
	val minutes = runtimeMinutes(runTimeTicks)
	val runtimeText = minutes?.let { total ->
		val hours = total / MINUTES_PER_HOUR
		val rest = total % MINUTES_PER_HOUR
		if (hours > 0) context.getString(R.string.ephyra_runtime_hours_minutes, hours, rest)
		else context.getString(R.string.ephyra_runtime_minutes, rest)
	}
	val endsAtText = endsAt(runTimeTicks, userData?.playbackPositionTicks, now)?.let {
		context.getString(R.string.lbl_playback_control_ends, context.getTimeFormatter().format(it))
	}

	return episodeMetaLine(runtimeText, communityRating, endsAtText)
}
