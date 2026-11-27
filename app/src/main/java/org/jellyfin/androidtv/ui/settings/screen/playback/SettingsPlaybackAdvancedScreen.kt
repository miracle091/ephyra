package org.jellyfin.androidtv.ui.settings.screen.playback

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.getQualityProfiles
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsAsyncActionListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.util.profile.createDeviceProfileReport
import org.jellyfin.preference.Preference
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.model.ServerVersion
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackAdvancedScreen() {
	val userPreferences = koinInject<UserPreferences>()
	val userSettingPreferences = koinInject<UserSettingPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback_advanced)) },
			)
		}

		customizationItems(userPreferences, userSettingPreferences)
		videoItems(userPreferences)
		subtitleItems(userPreferences)
		liveTvItems(userPreferences)
		audioItems(userPreferences)
		troubleshootingItems(userPreferences)
	}
}

private fun LazyListScope.customizationItems(userPreferences: UserPreferences, userSettingPreferences: UserSettingPreferences) {
	item { ListSection(headingContent = { Text(stringResource(R.string.pref_customization)) }) }

	item {
		val resumeSubtractDuration by rememberPreference(userPreferences, UserPreferences.resumeSubtractDuration)
		val options = getResumeSubtractDurationOptions()

		NavigationButton(
			heading = R.string.lbl_resume_preroll,
			route = Routes.PLAYBACK_RESUME_SUBTRACT_DURATION,
			caption = options[resumeSubtractDuration].orEmpty(),
		)
	}

	item {
		var skipForwardLength by rememberPreference(userSettingPreferences, UserSettingPreferences.skipForwardLength)

		SkipForwardLengthControl(value = skipForwardLength, onValueChange = { skipForwardLength = it })
	}

	item {
		val bufferLength by rememberPreference(userPreferences, UserPreferences.bufferLength)

		NavigationButton(
			heading = R.string.playback_buffer_length,
			route = Routes.PLAYBACK_BUFFER_LENGTH,
			caption = stringResource(bufferLength.nameRes),
		)
	}
}

private fun LazyListScope.videoItems(userPreferences: UserPreferences) {
	item { ListSection(headingContent = { Text(stringResource(R.string.pref_video)) }) }

	item {
		val maxBitrate by rememberPreference(userPreferences, UserPreferences.maxBitrate)
		val options = getQualityProfiles(LocalContext.current)

		NavigationButton(
			heading = R.string.pref_max_bitrate_title,
			route = Routes.PLAYBACK_MAX_BITRATE,
			caption = options[maxBitrate].orEmpty(),
		)
	}

	item {
		val refreshRateSwitchingBehavior by rememberPreference(userPreferences, UserPreferences.refreshRateSwitchingBehavior)

		NavigationButton(
			heading = R.string.lbl_refresh_switching,
			route = Routes.PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR,
			caption = stringResource(refreshRateSwitchingBehavior.nameRes),
		)
	}

	item {
		var videoStartDelay by rememberPreference(userPreferences, UserPreferences.videoStartDelay)

		VideoStartDelayControl(value = videoStartDelay, onValueChange = { videoStartDelay = it })
	}

	item {
		val playerZoomMode by rememberPreference(userPreferences, UserPreferences.playerZoomMode)

		NavigationButton(
			heading = R.string.default_video_zoom,
			route = Routes.PLAYBACK_ZOOM_MODE,
			caption = stringResource(playerZoomMode.nameRes),
		)
	}

	item {
		NavigationButton(
			heading = R.string.preference_hdr_overrides,
			route = Routes.PLAYBACK_HDR_OVERRIDES,
			caption = stringResource(R.string.preference_hdr_overrides_summary),
		)
	}

	item {
		NavigationButton(
			heading = R.string.preference_codecs,
			route = Routes.PLAYBACK_CODEC,
			caption = stringResource(R.string.preference_codecs_summary),
		)
	}
}

private fun LazyListScope.subtitleItems(userPreferences: UserPreferences) {
	item { ListSection(headingContent = { Text(stringResource(R.string.pref_subtitles)) }) }

	item {
		PreferenceCheckbox(
			store = userPreferences,
			preference = UserPreferences.pgsDirectPlay,
			heading = R.string.preference_enable_pgs,
			key = "pgs_direct_play",
			caption = R.string.preference_enable_pgs_description,
		)
	}

	item {
		PreferenceCheckbox(
			store = userPreferences,
			preference = UserPreferences.assDirectPlay,
			heading = R.string.preference_enable_ass,
			key = "ass_direct_play",
			caption = R.string.preference_enable_ass_description,
		)
	}

	item {
		PreferenceCheckbox(
			store = userPreferences,
			preference = UserPreferences.subtitlesBurnDuringTranscode,
			heading = R.string.pref_burn_subtitles_when_transcoding,
			key = "subtitles_burn_during_transcode",
			caption = R.string.pref_burn_subtitles_when_transcoding_description,
		)
	}
}

private fun LazyListScope.liveTvItems(userPreferences: UserPreferences) {
	item { ListSection(headingContent = { Text(stringResource(R.string.pref_live_tv_cat)) }) }

	item {
		PreferenceCheckbox(
			store = userPreferences,
			preference = UserPreferences.liveTvDirectPlayEnabled,
			heading = R.string.lbl_direct_stream_live,
			key = "live_tv_direct_play_enabled",
		)
	}
}

private fun LazyListScope.audioItems(userPreferences: UserPreferences) {
	item { ListSection(headingContent = { Text(stringResource(R.string.pref_audio)) }) }

	item {
		val audioBehaviour by rememberPreference(userPreferences, UserPreferences.audioBehaviour)

		NavigationButton(
			heading = R.string.lbl_audio_output,
			route = Routes.PLAYBACK_AUDIO_BEHAVIOR,
			caption = stringResource(audioBehaviour.nameRes),
		)
	}

	item {
		PreferenceCheckbox(
			store = userPreferences,
			preference = UserPreferences.audioNightMode,
			heading = R.string.pref_audio_night_mode,
			key = "audio_night_mode",
			caption = R.string.velella_audio_night_mode_content,
		)
	}

	item {
		PreferenceCheckbox(
			store = userPreferences,
			preference = UserPreferences.preferExoPlayerFfmpeg,
			heading = R.string.prefer_exoplayer_ffmpeg,
			key = "prefer_exoplayer_ffmpeg",
			caption = R.string.velella_prefer_ffmpeg_content,
		)
	}
}

private fun LazyListScope.troubleshootingItems(userPreferences: UserPreferences) {
	item { ListSection(headingContent = { Text(stringResource(R.string.pref_troubleshooting)) }) }

	item {
		val context = LocalContext.current
		val api = koinInject<ApiClient>()
		val serverVersion = koinInject<ServerVersion>()

		SettingsAsyncActionListButton(
			headingContent = { Text(stringResource(R.string.pref_report_device_profile_title)) },
			captionContent = { Text(stringResource(R.string.pref_report_device_profile_summary)) },
			action = {
				val report = createDeviceProfileReport(context, userPreferences, serverVersion)
				val response by api.systemApi.logFile(report)
				response
			},
			onResult = { result ->
				result.fold(
					onSuccess = { response ->
						Toast.makeText(
							context,
							@SuppressLint("LocalContextGetResourceValueCall")
							context.getString(R.string.pref_report_device_profile_success, response.fileName),
							Toast.LENGTH_LONG
						).show()
					},
					onFailure = {
						Toast.makeText(context, R.string.pref_report_device_profile_failure, Toast.LENGTH_LONG).show()
					},
				)
			},
			modifier = Modifier.focusKey("report_device_profile")
		)
	}
}

/** A button that opens [route], with the current value (or a description) as caption. */
@Composable
private fun NavigationButton(@StringRes heading: Int, route: String, caption: String) {
	val router = LocalRouter.current

	ListButton(
		headingContent = { Text(stringResource(heading)) },
		slots = ListItemSlots(
			captionContent = { Text(caption) },
		),
		onClick = { router.push(route) },
		modifier = Modifier.focusKey(route)
	)
}

/** A checkbox for a boolean user preference, with an optional caption. */
@Composable
private fun PreferenceCheckbox(
	store: UserPreferences,
	preference: Preference<Boolean>,
	@StringRes heading: Int,
	key: String,
	@StringRes caption: Int? = null,
) {
	var checked by rememberPreference(store, preference)

	ListButton(
		headingContent = { Text(stringResource(heading)) },
		slots = ListItemSlots(
			captionContent = if (caption != null) ({ Text(stringResource(caption)) }) else null,
			trailingContent = { Checkbox(checked = checked) },
		),
		onClick = { checked = !checked },
		modifier = Modifier.focusKey(key)
	)
}
