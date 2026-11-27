package org.jellyfin.androidtv.ui.player.video

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.ui.InteractionTrackerViewModel
import org.jellyfin.androidtv.ui.playback.nextup.NextUpItemData
import org.jellyfin.androidtv.ui.playback.nextup.NextUpOverlay
import org.jellyfin.androidtv.ui.playback.stillwatching.StillWatchingItemData
import org.jellyfin.androidtv.ui.playback.stillwatching.StillWatchingOverlay
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.androidtv.util.sdk.getDisplayName
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject

private const val SCRIM_ALPHA = 0.6f

/**
 * Velella: the legacy next up and still watching screens, shown over the paused last frame while the queue holds at the end of an
 * entry ([next], see [EndOfEntryPrompt]). Confirming plays [next]; cancelling, or still watching's countdown, closes the player.
 */
@Composable
fun EndOfEntryOverlay(
	playbackManager: PlaybackManager,
	prompt: EndOfEntryPrompt,
	next: QueueEntry,
	onClose: () -> Unit,
) {
	val item = next.baseItem ?: return
	val context = LocalContext.current
	val userPreferences = koinInject<UserPreferences>()
	val interactionTracker = koinInject<InteractionTrackerViewModel>()
	val coroutineScope = rememberCoroutineScope()
	val playNext: () -> Unit = {
		coroutineScope.launch { playbackManager.queue.next(usePlaybackOrder = true, useRepeatMode = false) }
	}
	val focusRequester = remember { FocusRequester() }
	val showThumbnail = userPreferences[UserPreferences.nextUpBehavior] == NextUpBehavior.EXTENDED

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black.copy(alpha = SCRIM_ALPHA))
	) {
		val modifier = Modifier
			.align(Alignment.BottomCenter)
			.focusRequester(focusRequester)
		when (prompt) {
			EndOfEntryPrompt.NEXT_UP -> NextUpOverlay(
				modifier = modifier,
				item = remember(item) { item.toNextUpItemData(context, showThumbnail) },
				onConfirm = playNext,
				onCancel = onClose,
			)

			EndOfEntryPrompt.STILL_WATCHING -> StillWatchingOverlay(
				modifier = modifier,
				item = remember(item) { item.toStillWatchingItemData(context, showThumbnail) },
				onConfirm = {
					interactionTracker.notifyStillWatching()
					playNext()
				},
				onCancel = onClose,
			)

			EndOfEntryPrompt.NONE -> Unit
		}
	}

	LaunchedEffect(focusRequester, prompt) {
		if (prompt != EndOfEntryPrompt.NONE) focusRequester.requestFocus()
	}
}

private fun BaseItemDto.toNextUpItemData(context: Context, showThumbnail: Boolean) = NextUpItemData(
	baseItem = this,
	id = id,
	title = getDisplayName(context),
	thumbnail = itemImages[ImageType.PRIMARY].takeIf { showThumbnail },
	logo = itemImages[ImageType.LOGO] ?: parentImages[ImageType.LOGO],
)

private fun BaseItemDto.toStillWatchingItemData(context: Context, showThumbnail: Boolean) = StillWatchingItemData(
	baseItem = this,
	id = id,
	title = getDisplayName(context),
	thumbnail = itemImages[ImageType.PRIMARY].takeIf { showThumbnail },
	logo = itemImages[ImageType.LOGO] ?: parentImages[ImageType.LOGO],
)
