package org.jellyfin.androidtv.data.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemKind
import java.time.Instant

class DataRefreshServiceTest : FunSpec({
	val stopped = Instant.parse("2026-11-27T20:00:00Z")

	test("a stopped episode refreshes the details and the TV rows") {
		val service = DataRefreshService()

		service.markPlaybackStopped(BaseItemKind.EPISODE, stopped)

		service.lastPlayback shouldBe stopped
		service.lastTvPlayback shouldBe stopped
		service.lastMoviePlayback shouldBe null
	}

	test("a stopped movie refreshes the details and the movie rows") {
		val service = DataRefreshService()

		service.markPlaybackStopped(BaseItemKind.MOVIE, stopped)

		service.lastPlayback shouldBe stopped
		service.lastMoviePlayback shouldBe stopped
		service.lastTvPlayback shouldBe null
	}

	test("another kind, or an unknown one, refreshes only the details") {
		val service = DataRefreshService()

		service.markPlaybackStopped(null, stopped)

		service.lastPlayback shouldBe stopped
		service.lastMoviePlayback shouldBe null
		service.lastTvPlayback shouldBe null
	}
})
