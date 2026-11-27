package org.jellyfin.androidtv.ui.player.video

import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.androidtv.util.sdk.end
import org.jellyfin.androidtv.util.sdk.start
import org.jellyfin.sdk.model.api.MediaSegmentDto
import kotlin.time.Duration

/**
 * Velella: where the skip button jumps to at [position]: the end of the segment playing there that the user set to "ask to skip"
 * ([actionOf], the segment settings), or null outside such a segment.
 */
fun askToSkipTarget(
	segments: List<MediaSegmentDto>,
	actionOf: (MediaSegmentDto) -> MediaSegmentAction,
	position: Duration,
): Duration? = segments
	.firstOrNull { segment -> position >= segment.start && position < segment.end && actionOf(segment) == MediaSegmentAction.ASK_TO_SKIP }
	?.end
