package org.jellyfin.playback.media3.exoplayer.mapping

import androidx.media3.common.MimeTypes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.core.mediastream.MediaStreamSubtitleTrack

private fun subtitle(index: Int, externalUrl: String? = null) = MediaStreamSubtitleTrack(
	codec = "subrip",
	index = index,
	language = null,
	title = null,
	isDefault = false,
	isForced = false,
	isExternal = externalUrl != null,
	externalUrl = externalUrl,
)

class SubtitleSelectionTests : FunSpec({
	// A file with two embedded subtitles (stream indexes 2 and 3) and one external file (index 5)
	val tracks = listOf(subtitle(2), subtitle(3), subtitle(5, "https://server/Videos/1/Subtitles/5/0/Stream.srt"))
	// Media3 lists the container's text tracks first, then the side-loaded files
	val groups = listOf(null, null, 5)

	test("no subtitle chosen by the server turns subtitles off") {
		findSubtitleSelection(tracks, null, groups, directPlay = true) shouldBe SubtitleSelection.Off
	}

	test("an embedded subtitle maps to its position among the container's text tracks") {
		findSubtitleSelection(tracks, 3, groups, directPlay = true) shouldBe SubtitleSelection.Group(1)
	}

	test("an external subtitle is found by its id") {
		findSubtitleSelection(tracks, 5, listOf(5, null, null), directPlay = true) shouldBe SubtitleSelection.Group(0)
	}

	test("a different number of embedded tracks keeps the player's choice") {
		findSubtitleSelection(tracks, 3, listOf(null, 5), directPlay = true) shouldBe SubtitleSelection.Keep
	}

	test("a converted stream's only subtitle is the chosen one") {
		findSubtitleSelection(tracks, 3, listOf(null), directPlay = false) shouldBe SubtitleSelection.Group(0)
	}

	test("a subtitle burnt into a converted stream has nothing to select") {
		findSubtitleSelection(tracks, 3, emptyList(), directPlay = false) shouldBe SubtitleSelection.Keep
	}

	test("an index the stream doesn't list keeps the player's choice") {
		findSubtitleSelection(tracks, 9, groups, directPlay = true) shouldBe SubtitleSelection.Keep
	}

	test("the external index survives Media3's id prefix") {
		externalSubtitleIndex(externalSubtitleId(5)) shouldBe 5
		externalSubtitleIndex("1:" + externalSubtitleId(12)) shouldBe 12
		externalSubtitleIndex("2") shouldBe null
		externalSubtitleIndex(null) shouldBe null
	}

	test("the MIME type comes from the file extension first, then the codec") {
		subtitleMimeType("subrip", "https://server/Videos/1/Subtitles/5/0/Stream.vtt?ApiKey=x") shouldBe MimeTypes.TEXT_VTT
		subtitleMimeType("subrip", null) shouldBe MimeTypes.APPLICATION_SUBRIP
		subtitleMimeType("ass", "https://server/Videos/1/Subtitles/5/0/Stream") shouldBe MimeTypes.TEXT_SSA
	}
})
