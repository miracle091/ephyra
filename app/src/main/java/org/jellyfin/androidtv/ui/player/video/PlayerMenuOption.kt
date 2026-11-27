package org.jellyfin.androidtv.ui.player.video

import org.jellyfin.androidtv.integration.dream.formatPlaybackTime
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamSubtitleTrack
import org.jellyfin.playback.core.mediastream.MediaStreamTrack
import org.jellyfin.sdk.model.api.ChapterInfo
import org.jellyfin.sdk.model.extensions.ticks
import kotlin.time.Duration

/** Velella: one entry of a menu of the video player's controls: what picking it selects, and its label. */
data class PlayerMenuOption<T>(val value: T, val label: String)

/** Velella: the audio tracks of the source by their index, labelled with the server's display name as in the legacy player. */
fun audioMenuOptions(tracks: Collection<MediaStreamTrack>): List<PlayerMenuOption<Int>> =
	tracks.filterIsInstance<MediaStreamAudioTrack>().mapNotNull { track ->
		val index = track.index ?: return@mapNotNull null
		PlayerMenuOption(index, track.title ?: track.codec.uppercase())
	}

/** Velella: "none" ([noneLabel], a null index) then the subtitle tracks of the source, as in the legacy player. */
fun subtitleMenuOptions(tracks: Collection<MediaStreamTrack>, noneLabel: String): List<PlayerMenuOption<Int?>> =
	listOf(PlayerMenuOption<Int?>(null, noneLabel)) + tracks.filterIsInstance<MediaStreamSubtitleTrack>().map { track ->
		PlayerMenuOption(track.index, track.title ?: track.language ?: track.codec.uppercase())
	}

/** Velella: the chapters by their start, labelled with their name and start time (only the time when they have no name). */
fun chapterMenuOptions(chapters: List<ChapterInfo>): List<PlayerMenuOption<Duration>> = chapters.map { chapter ->
	val start = chapter.startPositionTicks.ticks
	val time = formatPlaybackTime(start.inWholeMilliseconds)
	val name = chapter.name?.takeIf { it.isNotBlank() }
	PlayerMenuOption(start, if (name == null) time else "$name · $time")
}

/** Velella: the chapter playing at [position]: the last one that started at or before it. */
fun currentChapter(options: List<PlayerMenuOption<Duration>>, position: Duration): Duration? =
	options.lastOrNull { it.value <= position }?.value
