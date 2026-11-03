package org.jellyfin.androidtv.update

import android.content.Context
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

private fun updaterAnswering(code: Int, body: String): AppUpdater = updater { request ->
	Response.Builder()
		.request(request)
		.protocol(Protocol.HTTP_1_1)
		.code(code)
		.message("")
		.body(body.toResponseBody())
		.build()
}

private fun updater(respond: (Request) -> Response): AppUpdater {
	val request = slot<Request>()
	val httpClient = mockk<OkHttpClient> {
		every { newCall(capture(request)) } answers {
			mockk<Call> { every { execute() } answers { respond(request.captured) } }
		}
	}
	return AppUpdater(mockk<Context>(), httpClient, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
}

class AppUpdaterTest : FunSpec({
	test("A newer release is offered for installation") {
		val updater = updaterAnswering(200, "v99.0.0=99000099")
		updater.checkForUpdate()
		eventually(5.seconds) { updater.state.value shouldBe AppUpdateState.Available(AppUpdate("99.0.0", 99000099)) }
	}

	test("The installed version or an older one is up to date") {
		// Unit tests run the debug build, version 0.0.0-dev.1 (code 1)
		val updater = updaterAnswering(200, "v0.0.0-dev.1=1")
		updater.checkForUpdate()
		eventually(5.seconds) { updater.state.value shouldBe AppUpdateState.UpToDate }
	}

	test("The updater's client has no call timeout and stays on HTTPS") {
		val client = AppUpdater.httpClient(OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build())
		client.callTimeoutMillis shouldBe 0
		client.followSslRedirects shouldBe false
	}

	test("A missing release, a malformed file or a network error fails the check") {
		listOf(
			updaterAnswering(404, "Not Found"),
			updaterAnswering(200, "<html></html>"),
			updater { throw IOException("offline") },
			// Ended on plain HTTP after a redirect
			updater { request ->
				Response.Builder()
					.request(request.newBuilder().url("http://example.com/version.txt").build())
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("")
					.body("v99.0.0=99000099".toResponseBody())
					.build()
			},
		).forEach { updater ->
			updater.checkForUpdate()
			eventually(5.seconds) { updater.state.value shouldBe AppUpdateState.Failed }
		}
	}
})
