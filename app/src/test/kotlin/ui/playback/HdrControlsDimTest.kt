package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.data.compat.StreamInfo
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.VideoRange

class HdrControlsDimTest : FunSpec({
	fun stream(range: VideoRange?, method: PlayMethod = PlayMethod.DIRECT_PLAY) = StreamInfo().apply {
		playMethod = method
		mediaSource = mockk<MediaSourceInfo> {
			every { mediaStreams } returns listOfNotNull(
				mockk<MediaStream> { every { type } returns MediaStreamType.AUDIO },
				range?.let { r ->
					mockk<MediaStream> {
						every { type } returns MediaStreamType.VIDEO
						every { videoRange } returns r
					}
				},
			)
		}
	}

	test("an HDR video played or remuxed as it is counts as HDR") {
		HdrControlsDim.isHdr(stream(VideoRange.HDR)) shouldBe true
		HdrControlsDim.isHdr(stream(VideoRange.HDR, PlayMethod.DIRECT_STREAM)) shouldBe true
	}

	test("a transcode is SDR, the server tone-maps it") {
		HdrControlsDim.isHdr(stream(VideoRange.HDR, PlayMethod.TRANSCODE)) shouldBe false
	}

	test("SDR, unknown range, no video stream or no stream info are not HDR") {
		HdrControlsDim.isHdr(stream(VideoRange.SDR)) shouldBe false
		HdrControlsDim.isHdr(stream(VideoRange.UNKNOWN)) shouldBe false
		HdrControlsDim.isHdr(stream(null)) shouldBe false
		HdrControlsDim.isHdr(null) shouldBe false
	}
})
