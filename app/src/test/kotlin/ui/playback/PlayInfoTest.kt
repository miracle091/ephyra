package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.TranscodeReason

class PlayInfoTest : FunSpec({
	test("Reads the reasons from the transcoding URL") {
		val url = "/videos/1/master.m3u8?MediaSourceId=1&VideoCodec=hevc" +
			"&TranscodeReasons=VideoCodecNotSupported%2CAudioChannelsNotSupported&api_key=x"

		PlayInfo.transcodeReasons(url) shouldBe listOf(
			TranscodeReason.VIDEO_CODEC_NOT_SUPPORTED,
			TranscodeReason.AUDIO_CHANNELS_NOT_SUPPORTED,
		)
	}

	test("Reads an unencoded list as the last parameter") {
		PlayInfo.transcodeReasons("/videos/1/stream.mkv?Static=false&TranscodeReasons=ContainerNotSupported") shouldBe
			listOf(TranscodeReason.CONTAINER_NOT_SUPPORTED)
	}

	test("Skips reasons this SDK does not know") {
		PlayInfo.transcodeReasons("/x?TranscodeReasons=SomethingNew,VideoRangeTypeNotSupported") shouldBe
			listOf(TranscodeReason.VIDEO_RANGE_TYPE_NOT_SUPPORTED)
	}

	test("No reasons without the parameter or URL") {
		PlayInfo.transcodeReasons("/x?MediaSourceId=1").shouldBeEmpty()
		PlayInfo.transcodeReasons(null).shouldBeEmpty()
	}
})
