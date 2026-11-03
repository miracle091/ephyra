package org.jellyfin.androidtv.data.service

import android.content.Context
import android.util.DisplayMetrics
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.BackdropBehavior
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.operations.ImageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class BackdropSizeTest : FunSpec({
	test("Backdrops are requested from the server and decoded at screen size") {
		val metrics = mockk<DisplayMetrics>()
		metrics.widthPixels = 1920
		metrics.heightPixels = 1080
		val context = mockk<Context> {
			every { resources.displayMetrics } returns metrics
		}

		val queries = mutableListOf<Map<String, Any?>>()
		val api = mockk<ApiClient> {
			every { getOrCreateApi(ImageApi::class, any()) } answers { ImageApi(self as ApiClient) }
			every { createUrl(any(), any(), capture(queries), any()) } returns "http://server/backdrop"
		}

		val request = CompletableDeferred<ImageRequest>()
		val imageLoader = mockk<ImageLoader> {
			coEvery { execute(any()) } answers {
				request.complete(firstArg())
				mockk<ErrorResult> { every { image } returns null }
			}
		}

		val userPreferences = mockk<UserPreferences> {
			every { this@mockk[UserPreferences.backdropBehavior] } returns BackdropBehavior.BACKDROP_WITHOUT_BLUR
		}

		val service = BackgroundService(context, mockk(), api, userPreferences, imageLoader)
		service.setBackground(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE, backdropImageTags = listOf("tag")))

		queries.single()["maxWidth"] shouldBe 1920

		val loaded = withTimeout(5_000) { request.await() }
		loaded.sizeResolver.size() shouldBe Size(1920, 1080)
		loaded.precision shouldBe Precision.INEXACT
		loaded.scale shouldBe Scale.FILL
	}
})
