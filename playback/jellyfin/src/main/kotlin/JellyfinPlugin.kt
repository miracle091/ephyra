package org.jellyfin.playback.jellyfin

import androidx.lifecycle.Lifecycle
import org.jellyfin.playback.core.plugin.playbackPlugin
import org.jellyfin.playback.jellyfin.lyrics.LyricsPlayerService
import org.jellyfin.playback.jellyfin.mediasegment.MediaSegmentService
import org.jellyfin.playback.jellyfin.mediastream.JellyfinMediaStreamResolver
import org.jellyfin.playback.jellyfin.playsession.PlaySessionService
import org.jellyfin.playback.jellyfin.playsession.PlaySessionSocketService
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.MediaSegmentDto

fun jellyfinPlugin(
	api: ApiClient,
	deviceProfileBuilder: () -> DeviceProfile,
	// Velella: whether to skip a segment, from the user's segment settings; null for no skipping
	skipMediaSegment: ((MediaSegmentDto) -> Boolean)? = null,
	lifecycle: Lifecycle? = null,
	// Velella: the user's "burn in subtitles when transcoding" setting
	alwaysBurnInSubtitles: () -> Boolean = { false },
) = playbackPlugin {
	provide(JellyfinMediaStreamResolver(api, deviceProfileBuilder, alwaysBurnInSubtitles))

	val playSessionService = PlaySessionService(api)
	provide(playSessionService)
	provide(PlaySessionSocketService(api, playSessionService, lifecycle))

	provide(LyricsPlayerService(api))

	if (skipMediaSegment != null) provide(MediaSegmentService(api, skipMediaSegment))
}
