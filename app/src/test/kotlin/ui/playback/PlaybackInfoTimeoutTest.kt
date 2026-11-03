package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PlaybackInfoTimeoutTest : FunSpec({
	val clientInfo = ClientInfo("Ephyra", "1.0")
	val deviceInfo = DeviceInfo("device-id", "Shield")
	val api = mockk<ApiClient> {
		every { baseUrl } returns "http://server:8096"
		every { accessToken } returns "token"
		every { this@mockk.clientInfo } returns clientInfo
		every { this@mockk.deviceInfo } returns deviceInfo
		every { httpClientOptions } returns HttpClientOptions()
	}

	test("PlaybackInfo waits longer than the app's default timeout, so a live TV channel has time to tune") {
		PLAYBACK_INFO_TIMEOUT shouldBeGreaterThan HttpClientOptions().requestTimeout
		PLAYBACK_INFO_TIMEOUT shouldBeGreaterThan HttpClientOptions().socketTimeout
	}

	test("The client keeps the server, the user and the device, and only changes the response timeouts") {
		val options = slot<HttpClientOptions>()
		val result = mockk<ApiClient>()
		val jellyfin = mockk<Jellyfin> {
			every { createApi("http://server:8096", "token", clientInfo, deviceInfo, capture(options)) } returns result
		}

		api.withResponseTimeout(jellyfin, 2.minutes) shouldBe result

		options.captured.requestTimeout shouldBe 2.minutes
		options.captured.socketTimeout shouldBe 2.minutes
		options.captured.connectTimeout shouldBe HttpClientOptions().connectTimeout
		HttpClientOptions().requestTimeout shouldBe 30.seconds
	}
})
