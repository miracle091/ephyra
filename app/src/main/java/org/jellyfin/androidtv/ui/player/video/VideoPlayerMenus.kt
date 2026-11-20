package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
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
 * Ephyra: the legacy player's round menu buttons on the right of the controls, in its order: subtitles (when the source has any),
 * audio (with more than one track), chapters (when the item has any) and quality (the maximum bitrate; picking one asks the server
 * for a new stream under it, from the current position).
 */
@Composable
fun VideoPlayerMenus(
	playbackManager: PlaybackManager,
	userPreferences: UserPreferences = koinInject(),
) {
	val context = LocalContext.current
	val entry by rememberQueueEntry(playbackManager)
	val stream = entry?.run { mediaStreamFlow.collectAsState(mediaStream) }?.value
	val item = entry?.run { baseItemFlow.collectAsState(baseItem) }?.value
	val selectedTracks by playbackManager.state.selectedTracks.collectAsState()
	val tracks = stream?.tracks.orEmpty()

	val noneLabel = stringResource(R.string.lbl_none)
	val subtitles = remember(tracks, noneLabel) { subtitleMenuOptions(tracks, noneLabel) }
	if (subtitles.size > 1) {
		VideoPlayerMenuButton(
			icon = R.drawable.ic_select_subtitle,
			contentDescription = stringResource(R.string.lbl_subtitle_track),
			options = subtitles,
			selected = selectedTracks.subtitleIndex,
			onSelect = { index -> playbackManager.selectSubtitleTrack(index) },
		)
	}

	val audio = remember(tracks) { audioMenuOptions(tracks) }
	if (audio.size > 1) {
		VideoPlayerMenuButton(
			icon = R.drawable.ic_select_audio,
			contentDescription = stringResource(R.string.lbl_audio_track),
			options = audio,
			selected = selectedTracks.audioIndex,
			onSelect = { index -> playbackManager.selectAudioTrack(index) },
		)
	}

	val chapters = remember(item) { chapterMenuOptions(item?.chapters.orEmpty()) }
	if (chapters.isNotEmpty()) {
		VideoPlayerMenuButton(
			icon = R.drawable.ic_select_chapter,
			contentDescription = stringResource(R.string.lbl_chapters),
			options = chapters,
			selected = currentChapter(chapters, playbackManager.state.positionInfo.active),
			onSelect = { start -> playbackManager.state.seek(start) },
		)
	}

	val qualities = remember { getQualityProfiles(context).map { (value, label) -> PlayerMenuOption(value, label) } }
	var maxBitrate by rememberPreference(userPreferences, UserPreferences.maxBitrate)
	VideoPlayerMenuButton(
		icon = { BitrateBadge(maxBitrate, stringResource(R.string.lbl_quality_profile)) },
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

/**
 * Ephyra: the quality button shows the tier of the chosen maximum bitrate ("4K", "HD", "SD") in a small frame instead of an icon,
 * so it reads as quality and not as a screen, in the button's content colour (dark on the focused button).
 */
@Composable
private fun BitrateBadge(maxBitrate: String, contentDescription: String) {
	val color = LocalTextStyle.current.color
	Box(
		contentAlignment = Alignment.Center,
		modifier = Modifier
			.defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
			.semantics { this.contentDescription = contentDescription },
	) {
		Text(
			text = qualityTierText(maxBitrate),
			maxLines = 1,
			style = LocalTextStyle.current.merge(color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold),
			modifier = Modifier
				.border(1.5.dp, color, RoundedCornerShape(3.dp))
				.padding(horizontal = 3.dp, vertical = 1.dp),
		)
	}
}
