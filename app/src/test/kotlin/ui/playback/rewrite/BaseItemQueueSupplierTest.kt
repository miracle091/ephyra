package org.jellyfin.androidtv.ui.playback.rewrite

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.androidtv.ui.playback.SUBTITLES_OFF
import org.jellyfin.androidtv.ui.playback.TrackChoice
import org.jellyfin.playback.core.queue.startPosition
import org.jellyfin.playback.jellyfin.queue.audioStreamIndex
import org.jellyfin.playback.jellyfin.queue.mediaSourceId
import org.jellyfin.playback.jellyfin.queue.subtitleStreamIndex
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

	test("the tracks picked on the details screen go to the entry playback starts with, with their version") {
		val choice = TrackChoice(episodes[1].id, mediaSourceId = "version-2", audioIndex = 3, subtitleIndex = SUBTITLES_OFF)
		val supplier = RewriteMediaManager.BaseItemQueueSupplier(api, episodes, false, startIndex = 1, startTracks = choice)

		supplier.getItem(1)?.audioStreamIndex shouldBe 3
		supplier.getItem(1)?.subtitleStreamIndex shouldBe SUBTITLES_OFF
		supplier.getItem(1)?.mediaSourceId shouldBe "version-2"
		supplier.getItem(0)?.audioStreamIndex shouldBe null
		supplier.getItem(2)?.subtitleStreamIndex shouldBe null
	}

	test("a pick for another item is ignored") {
		val choice = TrackChoice(episodes[0].id, mediaSourceId = null, audioIndex = 3)
		val supplier = RewriteMediaManager.BaseItemQueueSupplier(api, episodes, false, startIndex = 1, startTracks = choice)

		supplier.getItem(1)?.audioStreamIndex shouldBe null
	}

	test("no resume point starts from the beginning") {
		val supplier = RewriteMediaManager.BaseItemQueueSupplier(api, episodes, false, startIndex = 0, startPosition = Duration.ZERO)

		supplier.getItem(0)?.startPosition shouldBe null
	}
})
