package org.jellyfin.androidtv.ui.player.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.getQualityProfiles
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.mediastream.mediaStreamFlow
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.baseItemFlow
import org.jellyfin.playback.jellyfin.selectAudioTrack
import org.jellyfin.playback.jellyfin.selectSubtitleTrack
import org.koin.compose.koinInject
import timber.log.Timber

/**
 * Velella: the menu buttons on the right of the controls: subtitles (when the source has any) and audio (with more than one
 * track) in sight, then "…" with chapters (when the item has any) and quality (the maximum bitrate; picking one asks the server
 * for a new stream under it, from the current position).
 */
@Composable
fun VideoPlayerMenus(
	playbackManager: PlaybackManager,
	// Velella: the menu its remote key asked for (Captions, Audio track); a request for a menu the video lacks is just dropped
	menuRequest: VideoPlayerKeyAction? = null,
	onMenuRequestHandled: () -> Unit = {},
	userPreferences: UserPreferences = koinInject(),
) {
	val entry by rememberQueueEntry(playbackManager)
	val stream = entry?.run { mediaStreamFlow.collectAsState(mediaStream) }?.value
	val item = entry?.run { baseItemFlow.collectAsState(baseItem) }?.value
	val selectedTracks by playbackManager.state.selectedTracks.collectAsState()
	val tracks = stream?.tracks.orEmpty()

	val noneLabel = stringResource(R.string.lbl_none)
	val subtitles = remember(tracks, noneLabel) { subtitleMenuOptions(tracks, noneLabel) }
	val audio = remember(tracks) { audioMenuOptions(tracks) }
	LaunchedEffect(menuRequest, subtitles, audio) {
		val available = when (menuRequest) {
			VideoPlayerKeyAction.SUBTITLES_MENU -> subtitles.size > 1
			VideoPlayerKeyAction.AUDIO_MENU -> audio.size > 1
			else -> true
		}
		if (!available) onMenuRequestHandled()
	}

	if (subtitles.size > 1) {
		VideoPlayerMenuButton(
			icon = { MenuIcon(R.drawable.ic_select_subtitle, stringResource(R.string.lbl_subtitle_track)) },
			options = subtitles,
			selected = selectedTracks.subtitleIndex,
			onSelect = { index -> playbackManager.selectSubtitleTrack(index) },
			onOpenRequest = onMenuRequestHandled.takeIf { menuRequest == VideoPlayerKeyAction.SUBTITLES_MENU },
		)
	}

	if (audio.size > 1) {
		VideoPlayerMenuButton(
			icon = { MenuIcon(R.drawable.ic_select_audio, stringResource(R.string.lbl_audio_track)) },
			options = audio,
			selected = selectedTracks.audioIndex,
			onSelect = { index -> playbackManager.selectAudioTrack(index) },
			onOpenRequest = onMenuRequestHandled.takeIf { menuRequest == VideoPlayerKeyAction.AUDIO_MENU },
		)
	}

	val chapters = remember(item) { chapterMenuOptions(item?.chapters.orEmpty()) }
	val chaptersLabel = stringResource(R.string.lbl_chapters)
	val chapterMenu = if (chapters.isEmpty()) null else {
		val current = currentChapter(chapters, playbackManager.state.positionInfo.active)
		PlayerSubmenu(
			label = chaptersLabel,
			current = chapters.firstOrNull { it.value == current }?.label,
			options = chapters,
			selected = current,
			onSelect = { start -> playbackManager.state.seek(start) },
		)
	}

	VideoPlayerMoreButton(listOfNotNull(chapterMenu, qualitySubmenu(playbackManager, userPreferences)))
}

/** Velella: the quality (maximum bitrate) menu; picking one asks the server for a new stream under it, from the current position. */
@Composable
private fun qualitySubmenu(playbackManager: PlaybackManager, userPreferences: UserPreferences): PlayerSubmenu<String> {
	val context = LocalContext.current
	val qualities = remember { getQualityProfiles(context).map { (value, label) -> PlayerMenuOption(value, label) } }
	var maxBitrate by rememberPreference(userPreferences, UserPreferences.maxBitrate)
	return PlayerSubmenu(
		label = stringResource(R.string.lbl_quality_profile),
		current = qualities.firstOrNull { it.value == maxBitrate }?.label,
		options = qualities,
		selected = maxBitrate,
		onSelect = { value ->
			// The same preference as the legacy player's quality menu and the settings: the next stream request reads it
			maxBitrate = value
			Timber.i("Maximum bitrate set to $value, asking for a new stream")
			playbackManager.reloadStream()
		},
	)
}
