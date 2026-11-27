package org.jellyfin.androidtv.ui.player.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.ui.playback.NextProgram
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.koin.compose.koinInject
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

/** Velella: the live TV channel playing in the video player, with the programme on air and the one after it. */
data class LiveChannelInfo(
	val channel: BaseItemDto,
	val program: BaseItemDto?,
	val next: BaseItemDto?,
)

// As the legacy player: the programme on air is looked up again once it ends, at most every 15 seconds
private val MIN_REFRESH_DELAY = 15.seconds
// A channel without guide data, or a failed lookup, is tried again now and then
private val UNKNOWN_REFRESH_DELAY = 5.minutes

/** Velella: how long to wait before looking the programme on air up again: until it ends at [end], kept between 15 s and 5 min. */
fun liveRefreshDelay(end: LocalDateTime?, now: LocalDateTime): Duration {
	if (end == null) return UNKNOWN_REFRESH_DELAY
	return java.time.Duration.between(now, end).toKotlinDuration().coerceIn(MIN_REFRESH_DELAY, UNKNOWN_REFRESH_DELAY)
}

/**
 * Velella: the live info of [item] when it is a live TV channel (null for anything else), from the server: the programme on air
 * and the next one, as the legacy player's header (PlaybackController.updateTvProgramInfo). [refreshKey] looks them up again at
 * once, after a recording is set or cancelled.
 */
@Composable
fun rememberLiveChannelInfo(
	item: BaseItemDto?,
	refreshKey: Int = 0,
	api: ApiClient = koinInject(),
): LiveChannelInfo? {
	val channel = item?.takeIf { it.type == BaseItemKind.TV_CHANNEL }
	return produceState<LiveChannelInfo?>(null, channel?.id, refreshKey) {
		if (channel == null) {
			value = null
			return@produceState
		}

		// The queue's channel already has the programme on air when the guide sent it; show that until the server answers
		if (value?.channel?.id != channel.id) value = LiveChannelInfo(channel, channel.currentProgram, null)
		while (true) {
			val program = currentProgramAfter(loadCurrentProgram(api, channel.id), value?.program, LocalDateTime.now())
			val next = program?.endDate?.let { end -> loadNextProgram(api, channel.id, end) }
			value = LiveChannelInfo(channel, program, next)
			delay(liveRefreshDelay(program?.endDate, LocalDateTime.now()))
		}
	}.value
}

/**
 * Velella: the programme on air after a lookup that gave [loaded]: the server's answer (null in a gap of the guide data), or after a
 * failed lookup the [previous] programme while it has not ended at [now].
 */
fun currentProgramAfter(loaded: Result<BaseItemDto?>, previous: BaseItemDto?, now: LocalDateTime): BaseItemDto? =
	loaded.getOrElse { previous?.takeIf { program -> program.endDate?.isAfter(now) == true } }

private suspend fun loadCurrentProgram(api: ApiClient, channelId: UUID): Result<BaseItemDto?> = try {
	Result.success(withContext(Dispatchers.IO) { api.liveTvApi.getChannel(channelId).content.currentProgram })
} catch (error: CancellationException) {
	throw error
} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
	Timber.w(error, "Unable to get the programme on air")
	Result.failure(error)
}

private suspend fun loadNextProgram(api: ApiClient, channelId: UUID, after: LocalDateTime): BaseItemDto? = try {
	val programs = withContext(Dispatchers.IO) {
		api.liveTvApi.getLiveTvPrograms(
			channelIds = listOf(channelId),
			enableImages = false,
			sortBy = setOf(ItemSortBy.START_DATE),
			minStartDate = after,
			limit = 3,
		).content.items
	}
	NextProgram.pick(programs, after)
} catch (error: CancellationException) {
	throw error
} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
	Timber.w(error, "Unable to get the next programme")
	null
}
