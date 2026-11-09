package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType

class TrackMatchingTest : FunSpec({
	fun track(index: Int, language: String?, title: String? = null, codec: String? = null, forced: Boolean = false) =
		TrackInfo(index, language, title, codec, forced)

	fun stream(index: Int, type: MediaStreamType, language: String?, title: String? = null, codec: String? = null, forced: Boolean = false) =
		mockk<MediaStream> {
			every { this@mockk.index } returns index
			every { this@mockk.type } returns type
			every { this@mockk.language } returns language
			every { this@mockk.title } returns title
			every { this@mockk.codec } returns codec
			every { isForced } returns forced
		}

	context("matchTrack") {
		val candidates = listOf(
			track(1, "rus", "DUB", "ac3"),
			track(2, "rus", "MVO LostFilm", "ac3"),
			track(3, "rus", "MVO LostFilm", "eac3"),
			track(4, "eng", "English", "ac3"),
		)

		test("returns null when no track has the language") {
			matchTrack(candidates, track(-1, "ita")) shouldBe null
		}

		test("with only a language it takes the first track of that language") {
			matchTrack(candidates, track(-1, "rus")) shouldBe 1
		}

		test("the title picks between tracks of the same language, whatever the case") {
			matchTrack(candidates, track(9, "rus", "mvo lostfilm", "ac3")) shouldBe 2
		}

		test("the codec breaks a tie between equal titles") {
			matchTrack(candidates, track(9, "rus", "MVO LostFilm", "eac3")) shouldBe 3
		}

		test("the language wins over the title") {
			matchTrack(candidates, track(9, "eng", "DUB", "ac3")) shouldBe 4
		}

		test("falls back to the first track of the language when the title is gone") {
			matchTrack(candidates, track(9, "rus", "Something else", "dts")) shouldBe 1
		}

		test("a tie goes to the preferred index") {
			matchTrack(candidates, track(-1, "rus", codec = "ac3"), preferredIndex = 2) shouldBe 2
			matchTrack(candidates, track(-1, "rus"), preferredIndex = 3) shouldBe 3
		}

		test("the forced flag counts more than the title") {
			val subtitles = listOf(
				track(1, "eng", "Full", "subrip"),
				track(2, "eng", "Forced", "subrip", forced = true),
			)
			matchTrack(subtitles, track(9, "eng", "Full", "subrip", forced = true)) shouldBe 2
			matchTrack(subtitles, track(9, "eng", "Full", "subrip", forced = false)) shouldBe 1
		}

		test("tracks without language only match a wanted track without language") {
			val unknown = listOf(track(1, null, "A"), track(2, null, "B"))
			matchTrack(unknown, track(9, null, "B")) shouldBe 2
			matchTrack(unknown, track(9, "eng", "B")) shouldBe null
		}
	}

	context("remembered tracks of the queue") {
		val streams = listOf(
			stream(0, MediaStreamType.VIDEO, null),
			stream(1, MediaStreamType.AUDIO, "rus", "DUB", "ac3"),
			stream(2, MediaStreamType.AUDIO, "rus", "MVO", "ac3"),
			stream(3, MediaStreamType.SUBTITLE, "rus", "Signs", "ass", forced = true),
			stream(4, MediaStreamType.SUBTITLE, "rus", "Full", "subrip"),
		)

		test("the audio track played in one episode is picked again in the next") {
			val queue = VideoQueueManager()
			queue.setLastPlayedAudioTrack(streams[2])

			// The next episode lists the tracks in another order
			val next = listOf(
				stream(0, MediaStreamType.VIDEO, null),
				stream(1, MediaStreamType.AUDIO, "rus", "MVO", "ac3"),
				stream(2, MediaStreamType.AUDIO, "rus", "DUB", "ac3"),
			)
			preferredAudioIndex(queue, next) shouldBe 1
		}

		test("audio falls back to nothing when the language is missing, so the server default stays") {
			val queue = VideoQueueManager()
			queue.setLastPlayedAudioTrack(streams[2])
			preferredAudioIndex(queue, listOf(stream(1, MediaStreamType.AUDIO, "eng"))) shouldBe null
			preferredAudioIndex(VideoQueueManager(), streams) shouldBe null
		}


		test("the subtitle track played in one episode is picked again, never an audio track") {
			val queue = VideoQueueManager()
			queue.setLastPlayedSubtitleTrack(streams[4])
			preferredSubtitleIndex(queue, streams, requestedIndex = 3) shouldBe 4
		}

		test("subtitles explicitly off pick nothing") {
			val queue = VideoQueueManager()
			queue.setLastPlayedSubtitleLanguageIsoCode("")
			preferredSubtitleIndex(queue, streams, requestedIndex = null) shouldBe null
		}

		test("clearing the queue forgets the tracks") {
			val queue = VideoQueueManager()
			queue.setLastPlayedAudioTrack(streams[1])
			queue.setLastPlayedSubtitleTrack(streams[3])
			queue.clearVideoQueue()
			queue.getLastPlayedAudioTrack() shouldBe null
			queue.getLastPlayedSubtitleTrack() shouldBe null
			queue.getLastPlayedAudioLanguageIsoCode() shouldBe null
		}
	}

	context("hasAudioStream") {
		val source = mockk<MediaSourceInfo> {
			every { mediaStreams } returns listOf(
				stream(1, MediaStreamType.AUDIO, "rus"),
				stream(2, MediaStreamType.SUBTITLE, "rus"),
			)
		}

		test("only audio streams with the index count") {
			hasAudioStream(source, 1) shouldBe true
			hasAudioStream(source, 2) shouldBe false
			hasAudioStream(source, 7) shouldBe false
			hasAudioStream(null, 1) shouldBe false
		}
	}
})
