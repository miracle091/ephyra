package org.jellyfin.playback.media3.exoplayer

import android.content.Context
import org.jellyfin.playback.core.plugin.playbackPlugin

/** Ephyra: [exoPlayerOptions] is read each time a player is built, so a changed setting applies from the next playback. */
fun exoPlayerPlugin(
	androidContext: Context,
	exoPlayerOptions: () -> ExoPlayerOptions = { ExoPlayerOptions() },
) = playbackPlugin {
	provide(ExoPlayerBackend(androidContext, exoPlayerOptions))
}
