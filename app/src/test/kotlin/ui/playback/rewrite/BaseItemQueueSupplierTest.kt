package org.jellyfin.androidtv.ui.playback.rewrite

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.playback.core.queue.startPosition
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class BaseItemQueueSupplierTest : FunSpec({
	val api = mockk<ApiClient>(relaxed = true)
	val episodes = List(3) { BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE) }

	test("the resume point goes to the entry playback starts with, and only to it") {
		val supplier = RewriteMediaManager.BaseItemQueueSupplier(api, episodes, false, startIndex = 1, startPosition = 12.minutes)

		supplier.getItem(0)?.startPosition shouldBe null
		supplier.getItem(1)?.startPosition shouldBe 12.minutes
		supplier.getItem(2)?.startPosition shouldBe null
	}

	test("no resume point starts from the beginning") {
		val supplier = RewriteMediaManager.BaseItemQueueSupplier(api, episodes, false, startIndex = 0, startPosition = Duration.ZERO)

		supplier.getItem(0)?.startPosition shouldBe null
	}
})
