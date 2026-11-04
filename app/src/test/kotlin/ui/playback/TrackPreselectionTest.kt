package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.data.compat.VideoOptions
import org.jellyfin.androidtv.ui.itemdetail.DetailsTrackButtons
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import java.util.UUID

class TrackPreselectionTest : FunSpec({
	fun stream(index: Int, type: MediaStreamType, language: String?) = mockk<MediaStream> {
		every { this@mockk.index } returns index
		every { this@mockk.type } returns type
		every { this@mockk.language } returns language
	}

	fun source(id: String, vararg streams: MediaStream) = mockk<MediaSourceInfo> {
		every { this@mockk.id } returns id
		every { mediaStreams } returns streams.toList()
	}

	val itemId = UUID.randomUUID()
	// Two English audio tracks (film and commentary), one Italian; subtitles in Italian and English
	val main = source(
		itemId.toString().replace("-", ""),
		stream(0, MediaStreamType.VIDEO, null),
		stream(1, MediaStreamType.AUDIO, "eng"),
		stream(2, MediaStreamType.AUDIO, "eng"),
		stream(3, MediaStreamType.AUDIO, "ita"),
		stream(4, MediaStreamType.SUBTITLE, "ita"),
		stream(5, MediaStreamType.SUBTITLE, "eng"),
	)
	val other = source("other", stream(0, MediaStreamType.VIDEO, null), stream(1, MediaStreamType.AUDIO, "eng"))
	fun movie(vararg sources: MediaSourceInfo) = BaseItemDto(id = itemId, type = BaseItemKind.MOVIE, mediaSources = sources.toList())

	test("the exact picked tracks go into the request, with their languages saved") {
		val preselection = TrackPreselection()
		val queue = VideoQueueManager()
		preselection.pickAudio(itemId, main.id, 2)
		preselection.pickSubtitle(itemId, main.id, 5)

		val options = VideoOptions()
		applyTrackPreselection(options, movie(main), main, preselection, queue)

		options.audioStreamIndex shouldBe 2
		options.subtitleStreamIndex shouldBe 5
		queue.getLastPlayedAudioLanguageIsoCode() shouldBe "eng"
		queue.getLastPlayedSubtitleLanguageIsoCode() shouldBe "eng"
	}

	test("subtitles off are sent as -1 and saved as an empty language") {
		val preselection = TrackPreselection()
		val queue = VideoQueueManager()
		preselection.pickSubtitle(itemId, main.id, SUBTITLES_OFF)

		val options = VideoOptions().apply { audioStreamIndex = 3 }
		applyTrackPreselection(options, movie(main), main, preselection, queue)

		options.subtitleStreamIndex shouldBe SUBTITLES_OFF
		options.audioStreamIndex shouldBe 3 // not picked: left as the player set it
		queue.getLastPlayedSubtitleLanguageIsoCode() shouldBe ""
	}

	test("a pick applies once, and only to its item and version") {
		val preselection = TrackPreselection()
		val queue = VideoQueueManager()
		preselection.pickAudio(itemId, main.id, 3)

		// An intro or another version plays first: the pick waits
		val intro = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.TRAILER)
		VideoOptions().also { applyTrackPreselection(it, intro, other, preselection, queue) }.audioStreamIndex shouldBe null
		VideoOptions().also { applyTrackPreselection(it, movie(main), other, preselection, queue) }.audioStreamIndex shouldBe null

		VideoOptions().also { applyTrackPreselection(it, movie(main), main, preselection, queue) }.audioStreamIndex shouldBe 3
		VideoOptions().also { applyTrackPreselection(it, movie(main), main, preselection, queue) }.audioStreamIndex shouldBe null
	}

	test("the played media source is the one with the item's id, else the first") {
		movie(other, main).playedMediaSource shouldBe main
		movie(other).playedMediaSource shouldBe other
		BaseItemDto(id = itemId, type = BaseItemKind.MOVIE).playedMediaSource shouldBe null
	}

	test("the details screen offers audio with several audio tracks and subtitles with any subtitle track") {
		DetailsTrackButtons.trackTypesFor(movie(main), builtInLegacyPlayer = true) shouldBe
			listOf(MediaStreamType.AUDIO, MediaStreamType.SUBTITLE)
		DetailsTrackButtons.trackTypesFor(movie(other), builtInLegacyPlayer = true) shouldBe emptyList()
		DetailsTrackButtons.trackTypesFor(movie(main), builtInLegacyPlayer = false) shouldBe emptyList()
		DetailsTrackButtons.trackTypesFor(
			BaseItemDto(id = itemId, type = BaseItemKind.SERIES, mediaSources = listOf(main)),
			builtInLegacyPlayer = true,
		) shouldBe emptyList()
	}
})
