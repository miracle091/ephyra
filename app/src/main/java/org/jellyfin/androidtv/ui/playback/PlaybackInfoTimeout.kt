package org.jellyfin.androidtv.ui.playback

import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Time the server gets to answer a PlaybackInfo request. Opening a live TV channel makes the server tune it before it answers,
 * which can take longer than the default 30 second timeout of the app's client: the request then fails on the first try.
 */
val PLAYBACK_INFO_TIMEOUT = 2.minutes

/**
 * A client for the same server and user as [this], waiting up to [timeout] for the response instead of the app's default.
 */
fun ApiClient.withResponseTimeout(jellyfin: Jellyfin, timeout: Duration): ApiClient = jellyfin.createApi(
	baseUrl = baseUrl,
	accessToken = accessToken,
	clientInfo = clientInfo,
	deviceInfo = deviceInfo,
	httpClientOptions = httpClientOptions.copy(
		requestTimeout = timeout,
		socketTimeout = timeout,
	),
)
