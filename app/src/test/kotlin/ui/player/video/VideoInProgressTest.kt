package org.jellyfin.androidtv.ui.player.video

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.core.mediastream.mediatype.MediaType
import org.jellyfin.playback.core.mediastream.mediatype.mediaType
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.QueueEntry

class VideoInProgressTest : FunSpec({
	fun entry(type: MediaType) = QueueEntry().apply { mediaType = type }

	test("a video playing or paused in the new player is in progress") {
		isVideoInProgress(PlayState.PLAYING, entry(MediaType.Video)) shouldBe true
		isVideoInProgress(PlayState.PAUSED, entry(MediaType.Video)) shouldBe true
	}

	test("music, a stopped player or no entry don't block another client's display content") {
		isVideoInProgress(PlayState.PLAYING, entry(MediaType.Audio)) shouldBe false
		isVideoInProgress(PlayState.STOPPED, entry(MediaType.Video)) shouldBe false
		isVideoInProgress(PlayState.PLAYING, null) shouldBe false
	}

	test("only a paused video counts as paused for the screensaver") {
		isVideoPaused(PlayState.PAUSED, entry(MediaType.Video)) shouldBe true
		isVideoPaused(PlayState.PLAYING, entry(MediaType.Video)) shouldBe false
		isVideoPaused(PlayState.PAUSED, entry(MediaType.Audio)) shouldBe false
	}
})
