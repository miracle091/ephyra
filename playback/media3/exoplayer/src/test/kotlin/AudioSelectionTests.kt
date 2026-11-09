package org.jellyfin.playback.media3.exoplayer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.playback.media3.exoplayer.mapping.findAudioGroupPosition

private fun audio(index: Int?, isExternal: Boolean = false) = MediaStreamAudioTrack(
	codec = "ac3",
	bitrate = 0,
	channels = 2,
	sampleRate = 48000,
	index = index,
	isExternal = isExternal,
)

private val video = MediaStreamVideoTrack(codec = "h264", bitrate = 0, width = 1920, height = 1080, videoRange = null)

class AudioSelectionTests : FunSpec({
	test("Position counts audio tracks only, in stream index order") {
		// Video is stream 0, audio 1 (AC3) and 2 (TrueHD)
		val tracks = listOf(video, audio(1), audio(2))

		findAudioGroupPosition(tracks, streamIndex = 1, playerAudioGroupCount = 2) shouldBe 0
		findAudioGroupPosition(tracks, streamIndex = 2, playerAudioGroupCount = 2) shouldBe 1
	}

	test("Position is sorted by stream index, not by list order") {
		val tracks = listOf(audio(3), audio(1), audio(2))

		findAudioGroupPosition(tracks, streamIndex = 3, playerAudioGroupCount = 3) shouldBe 2
	}

	test("External audio tracks are not counted") {
		val tracks = listOf(video, audio(1), audio(2), audio(5, isExternal = true))

		findAudioGroupPosition(tracks, streamIndex = 2, playerAudioGroupCount = 2) shouldBe 1
		findAudioGroupPosition(tracks, streamIndex = 5, playerAudioGroupCount = 2) shouldBe null
	}

	test("Null when the player shows another number of audio tracks") {
		val tracks = listOf(video, audio(1), audio(2))

		findAudioGroupPosition(tracks, streamIndex = 2, playerAudioGroupCount = 1) shouldBe null
	}

	test("Null for an unknown stream index") {
		findAudioGroupPosition(listOf(video, audio(1)), streamIndex = 7, playerAudioGroupCount = 1) shouldBe null
	}
})
