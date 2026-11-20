package org.jellyfin.androidtv.integration.dream

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.playback.core.mediastream.mediatype.MediaType
import org.jellyfin.playback.core.mediastream.mediatype.mediaType
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class PausedVideoContentTest : FunSpec({
	val film = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE)
	fun entry(type: MediaType) = QueueEntry().apply {
		baseItem = film
		mediaType = type
	}
	val position = { PositionInfo(active = 24.minutes, buffer = Duration.ZERO, duration = 90.minutes) }

	test("a video paused in the new player shows in the screensaver with its position") {
		pausedVideoContent(PlayState.PAUSED, entry(MediaType.Video), position) shouldBe
			DreamContent.VideoPaused(film, 24.minutes.inWholeMilliseconds, 90.minutes.inWholeMilliseconds)
	}

	test("a playing video, paused music or no entry give no paused video") {
		pausedVideoContent(PlayState.PLAYING, entry(MediaType.Video), position) shouldBe null
		pausedVideoContent(PlayState.PAUSED, entry(MediaType.Audio), position) shouldBe null
		pausedVideoContent(PlayState.PAUSED, null, position) shouldBe null
	}
})
