package org.jellyfin.playback.media3.exoplayer

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.playback.core.model.PositionInfo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ExoPlayerBackendLifecycleTests : FunSpec({
	test("polling and controls without a player neither build one nor read the options") {
		var optionReads = 0
		val backend = ExoPlayerBackend(mockk<Context>()) {
			optionReads++
			ExoPlayerOptions()
		}

		backend.getPositionInfo() shouldBe PositionInfo(Duration.ZERO, Duration.ZERO, Duration.ZERO)
		backend.pause()
		backend.seekTo(10.seconds)
		backend.setScrubbing(true)
		backend.setSpeed(1.5f)
		backend.selectAudioTrack(2) shouldBe false
		backend.selectSubtitleTrack(3) shouldBe false
		backend.selectSubtitleTrack(null) shouldBe false
		backend.setSurfaceView(null)
		backend.setSubtitleView(null)
		backend.stop()

		optionReads shouldBe 0
	}
})
