package org.jellyfin.playback.core.mediastream

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jellyfin.playback.core.model.PlayState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Velella: how long a live stream may take to start playing once the player has it. Some channels leave the player buffering
 * forever without an error. On the Shield, cold channels showed their first frame up to 34 s after the stream was handed to the
 * player (guide, 2026-11-03) and 24 s on 2026-11-27, and a retry starts the server's transcode over: 20 s (the legacy player's
 * `LiveStreamRetry`) made such channels never start. 45 s lets them start, and the retry chain (three attempts) still closes a
 * dead channel in about two minutes.
 */
internal val LIVE_START_TIMEOUT = 45.seconds

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
