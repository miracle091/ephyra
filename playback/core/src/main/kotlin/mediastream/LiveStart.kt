package org.jellyfin.playback.core.mediastream

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jellyfin.playback.core.model.PlayState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Velella: how long a live stream may take to start playing. Some channels leave the player buffering forever without an error;
 * a transcoded channel took about 15 s to start on the Shield, so 20 s leaves some room and, with the retry chain, closes a dead
 * channel in about a minute (as the legacy player's `LiveStreamRetry`).
 */
internal val LIVE_START_TIMEOUT = 20.seconds

/**
 * Velella: whether a live stream that was just handed to the player stalled: it neither played nor failed within [timeout]. A
 * failure is left to the retry chain, which already reacts to it. The error of the stream a retry replaces is still the state when
 * the new stream starts, so it is skipped.
 */
internal suspend fun liveStartStalled(playState: Flow<PlayState>, timeout: Duration = LIVE_START_TIMEOUT): Boolean =
	withTimeoutOrNull(timeout) {
		playState
			.dropWhile { it == PlayState.ERROR }
			.first { it == PlayState.PLAYING || it == PlayState.ERROR }
	} == null
