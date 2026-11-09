package org.jellyfin.playback.media3.exoplayer.shield

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.media3.exoplayer.shield.BitstreamFirstRenderersFactory.Companion.shouldFfmpegStepAside

@OptIn(UnstableApi::class)
class BitstreamFirstRenderersFactoryTests : FunSpec({
	val bitstreamMimeTypes = listOf(
		MimeTypes.AUDIO_TRUEHD,
		MimeTypes.AUDIO_DTS,
		MimeTypes.AUDIO_DTS_HD,
		MimeTypes.AUDIO_AC3,
		MimeTypes.AUDIO_E_AC3,
		MimeTypes.AUDIO_E_AC3_JOC,
	)

	bitstreamMimeTypes.forEach { mimeType ->
		test("FFmpeg steps aside for $mimeType when the sink passes it through") {
			shouldFfmpegStepAside(mimeType, AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY) shouldBe true
		}

		test("FFmpeg keeps $mimeType when the sink cannot pass it through") {
			shouldFfmpegStepAside(mimeType, AudioSink.SINK_FORMAT_UNSUPPORTED) shouldBe false
			shouldFfmpegStepAside(mimeType, AudioSink.SINK_FORMAT_SUPPORTED_WITH_TRANSCODING) shouldBe false
		}
	}

	test("DTS-HD that would pass through only as the DTS core goes to FFmpeg, full DTS-HD passthrough does not") {
		shouldFfmpegStepAside(MimeTypes.AUDIO_DTS_HD, AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY, C.ENCODING_DTS) shouldBe false
		shouldFfmpegStepAside(MimeTypes.AUDIO_DTS_HD, AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY, C.ENCODING_DTS_HD) shouldBe true
		shouldFfmpegStepAside(MimeTypes.AUDIO_DTS, AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY, C.ENCODING_DTS) shouldBe true
	}

	test("FFmpeg never steps aside for PCM or an unknown MIME type") {
		shouldFfmpegStepAside(MimeTypes.AUDIO_RAW, AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY) shouldBe false
		shouldFfmpegStepAside(null, AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY) shouldBe false
	}
})
