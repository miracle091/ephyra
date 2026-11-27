package org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackMediaSegmentsScreen() {
	val router = LocalRouter.current
	val mediaSegmentRepository = koinInject<MediaSegmentRepository>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback_media_segments)) },
			)
		}

		item {
			// Velella: how long the skip button stays on screen
			val velellaPreferences = koinInject<VelellaPreferences>()
			val skipAutoHideSeconds by rememberPreference(velellaPreferences, VelellaPreferences.skipAutoHideSeconds)

			ListButton(
				headingContent = { Text(stringResource(R.string.velella_pref_skip_auto_hide)) },
				slots = ListItemSlots(
					captionContent = { Text(skipAutoHideLabel(skipAutoHideSeconds)) },
				),
				onClick = { router.push(Routes.PLAYBACK_MEDIA_SEGMENTS_SKIP_DURATION) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_MEDIA_SEGMENTS_SKIP_DURATION)
			)
		}

		items(MediaSegmentRepository.SupportedTypes) { segmentType ->
			val action = mediaSegmentRepository.getDefaultSegmentTypeAction(segmentType)

			ListButton(
				headingContent = { Text(stringResource(segmentType.nameRes)) },
				slots = ListItemSlots(
					captionContent = { Text(stringResource(action.nameRes)) },
				),
				onClick = {
					router.push(
						route = Routes.PLAYBACK_MEDIA_SEGMENT,
						parameters = mapOf(
							"segmentType" to segmentType.toString(),
						),
					)
				},
				modifier = Modifier.focusKey("media_segment_type_$segmentType")
			)
		}
	}
}
