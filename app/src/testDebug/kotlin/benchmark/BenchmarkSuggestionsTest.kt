package org.jellyfin.androidtv.benchmark

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.jellyfin.sdk.model.api.PlayMethod

private fun smooth(clip: String, hardware: Boolean = true) = DecoderResult(clip, hardware = hardware, renderedFrames = 600)
private fun stuttering(clip: String) = DecoderResult(clip, hardware = false, renderedFrames = 400, droppedFrames = 200)

class BenchmarkSuggestionsTest : FunSpec({
	test("Smooth 1080p software AV1 suggests turning software codecs on") {
		val suggestions = benchmarkSuggestions(listOf(smooth("1080p AV1 10bit 10M", hardware = false)), emptyList())
		suggestions shouldHaveSize 1
		suggestions[0] shouldStartWith "Software AV1 up to 1080p is smooth"
	}

	test("Stuttering AV1 keeps software codecs off and the 1080p cap") {
		val suggestions = benchmarkSuggestions(listOf(stuttering("1080p AV1 10bit 10M"), stuttering("4K AV1 10bit 100M")), emptyList())
		suggestions shouldHaveSize 2
		suggestions[0] shouldContain "keep software codecs off"
		suggestions[1] shouldContain "keep the 1080p cap"
		suggestions[1] shouldContain "200/600 dropped"
	}

	test("Dolby Vision needs a smooth hardware decode") {
		benchmarkSuggestions(listOf(smooth("4K DV P8.1")), emptyList())[0] shouldStartWith "Dolby Vision plays on a hardware decoder"
		benchmarkSuggestions(listOf(smooth("4K DV P5", hardware = false)), emptyList())[0] shouldContain "HDR10 fallback"
	}

	test("Transcodes to H.264 suggest HEVC encoding on the server") {
		val server = listOf(ServerResult("4K AV1 HDR10 100M", PlayMethod.TRANSCODE, transcodeVideoCodec = "h264"))
		benchmarkSuggestions(emptyList(), server)[0] shouldContain "allow HEVC encoding"
	}

	test("Nothing to suggest without AV1, Dolby Vision or H.264 transcodes") {
		benchmarkSuggestions(listOf(smooth("1080p AVC 10M")), listOf(ServerResult("1080p AVC 10M", PlayMethod.DIRECT_PLAY))).shouldBeEmpty()
	}
})
