package org.jellyfin.androidtv.di

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.androidtv.ui.playback.TrackPreselection
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.ui.playback.rewrite.RewriteMediaManager
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.ui.player.base.subtitleStyle
import org.jellyfin.androidtv.ui.player.display.videoChangeFrameRateStrategy
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.util.profile.createDeviceProfile
import org.jellyfin.playback.core.playbackManager
import org.jellyfin.playback.jellyfin.jellyfinPlugin
import org.jellyfin.playback.media3.exoplayer.ExoPlayerOptions
import org.jellyfin.playback.media3.exoplayer.exoPlayerPlugin
import org.jellyfin.playback.media3.session.MediaSessionOptions
import org.jellyfin.playback.media3.session.media3SessionPlugin
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope
import org.koin.dsl.module
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.jellyfin.androidtv.ui.playback.PlaybackManager as LegacyPlaybackManager

val playbackModule = module {
	single { LegacyPlaybackManager(get(), get()) }
	single { VideoQueueManager() }
	single { TrackPreselection() } // Velella: tracks picked on the details screen
	single<MediaManager> { RewriteMediaManager(get(), get()) }

	single { PlaybackLauncher(get(), get(), get(), get()) }

	single<HttpDataSource.Factory> {
		val okHttpFactory = get<OkHttpFactory>()
		val httpClientOptions = get<HttpClientOptions>().copy(
			// Disable request timeout for media playback as this causes issues with Live TV
			requestTimeout = Duration.ZERO
		)

		OkHttpDataSource.Factory(okHttpFactory.createClient(httpClientOptions))
	}

	single { createPlaybackManager() }
}

fun Scope.createPlaybackManager() = playbackManager(androidContext()) {
	val activityIntent = Intent(get(), MainActivity::class.java)
	val pendingIntent = PendingIntent.getActivity(get(), 0, activityIntent, PendingIntent.FLAG_IMMUTABLE)

	val notificationChannelId = "session"
	if (AndroidVersion.isAtLeastO) {
		val channel = NotificationChannel(
			notificationChannelId,
			notificationChannelId,
			NotificationManager.IMPORTANCE_LOW
		)
		channel.setShowBadge(false)
		NotificationManagerCompat.from(get()).createNotificationChannel(channel)
	}

	val userPreferences = get<UserPreferences>()
	// Velella: read per player, so a setting changed after the first playback still applies
	val httpDataSourceFactory = get<HttpDataSource.Factory>()
	install(exoPlayerPlugin(get()) { createExoPlayerOptions(userPreferences, httpDataSourceFactory) })

	val mediaSessionOptions = MediaSessionOptions(
		channelId = notificationChannelId,
		notificationId = 1,
		iconSmall = R.drawable.app_icon_foreground,
		openIntent = pendingIntent,
	)
	install(media3SessionPlugin(get(), mediaSessionOptions))

	val deviceProfileBuilder = { createDeviceProfile(androidContext(), userPreferences) }
	// Velella: segments skipped as the user set them up (was: intros always); "ask to skip" waits for the new player's skip button
	val mediaSegmentRepository = get<MediaSegmentRepository>()
	install(
		jellyfinPlugin(
			get(),
			deviceProfileBuilder,
			skipMediaSegment = { segment -> mediaSegmentRepository.getMediaSegmentAction(segment) == MediaSegmentAction.SKIP },
			lifecycle = ProcessLifecycleOwner.get().lifecycle,
			alwaysBurnInSubtitles = { userPreferences[UserPreferences.subtitlesBurnDuringTranscode] },
		)
	)

	// Options
	val userSettingPreferences = get<UserSettingPreferences>()
	defaultRewindAmount = { userSettingPreferences[UserSettingPreferences.skipBackLength].milliseconds }
	defaultFastForwardAmount = { userSettingPreferences[UserSettingPreferences.skipForwardLength].milliseconds }
}

/** Velella: the ExoPlayer options from the user's current settings; called for every new player. */
internal fun createExoPlayerOptions(userPreferences: UserPreferences, httpDataSourceFactory: HttpDataSource.Factory): ExoPlayerOptions {
	val bufferLength = userPreferences[UserPreferences.bufferLength]
	return ExoPlayerOptions(
		preferFfmpeg = userPreferences[UserPreferences.preferExoPlayerFfmpeg],
		enableLibass = userPreferences[UserPreferences.assDirectPlay],
		enableDebugLogging = userPreferences[UserPreferences.debuggingEnabled],
		baseDataSourceFactory = httpDataSourceFactory,
		minBufferDuration = bufferLength.minBufferDuration,
		maxBufferDuration = bufferLength.maxBufferDuration,
		bufferForPlaybackDuration = bufferLength.bufferForPlaybackDuration,
		bufferForPlaybackAfterRebufferDuration = bufferLength.bufferForPlaybackAfterRebufferDuration,
		videoChangeFrameRateStrategy = videoChangeFrameRateStrategy(userPreferences[UserPreferences.refreshRateSwitchingBehavior]),
		subtitleStyle = userPreferences.subtitleStyle(),
		audioNightMode = userPreferences[UserPreferences.audioNightMode],
	)
}
