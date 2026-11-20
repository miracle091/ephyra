package org.jellyfin.androidtv.ui.player.video

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamSubtitleTrack
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.sdk.model.api.ChapterInfo
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PlayerMenuOptionTest : FunSpec({
	fun subtitle(index: Int, title: String?, language: String?) =
		MediaStreamSubtitleTrack("subrip", index, language, title, isDefault = false, isForced = false, isExternal = false, null)

	val tracks = listOf(
		MediaStreamVideoTrack("hevc", 0, 3840, 2160, "SDR"),
		MediaStreamAudioTrack("ac3", 0, 6, 48000, index = 1, title = "Italiano - Dolby Digital - 5.1"),
		MediaStreamAudioTrack("aac", 0, 2, 48000, index = 2),
		MediaStreamAudioTrack("dts", 0, 6, 48000, index = null, title = "Unknown"),
		subtitle(3, "Italiano - SUBRIP", "ita"),
		subtitle(4, null, "eng"),
	)

	test("audio tracks are listed by index with the server's name, the codec when it has none") {
		audioMenuOptions(tracks) shouldBe listOf(
			PlayerMenuOption(1, "Italiano - Dolby Digital - 5.1"),
			PlayerMenuOption(2, "AAC"),
		)
	}

	test("subtitles start with none, then each track by its name or language") {
		subtitleMenuOptions(tracks, "None") shouldBe listOf(
			PlayerMenuOption(null, "None"),
			PlayerMenuOption(3, "Italiano - SUBRIP"),
			PlayerMenuOption(4, "eng"),
		)
	}

	test("chapters show their name and start, only the start when unnamed") {
		fun chapter(start: Duration, name: String?) =
			ChapterInfo(start.inWholeMicroseconds * 10, name, imagePath = null, imageDateModified = LocalDateTime.MIN, imageTag = null)
		val chapters = listOf(chapter(0.seconds, "Opening"), chapter(5.minutes, " "))

		chapterMenuOptions(chapters) shouldBe listOf(
			PlayerMenuOption(0.seconds, "Opening · 0:00"),
			PlayerMenuOption(5.minutes, "5:00"),
		)
	}

	test("the quality badge shows 4K from 40 Mbps, HD from 8 Mbps, SD below") {
		qualityTierText("200") shouldBe "4K"
		qualityTierText("40") shouldBe "4K"
		qualityTierText("30") shouldBe "HD"
		qualityTierText("10") shouldBe "HD"
		qualityTierText("5") shouldBe "SD"
		qualityTierText("0.42") shouldBe "SD"
	}

	test("the current chapter is the last one started") {
		val options = listOf(PlayerMenuOption(0.seconds, "a"), PlayerMenuOption(5.minutes, "b"))

		currentChapter(options, 4.minutes) shouldBe 0.seconds
		currentChapter(options, 5.minutes) shouldBe 5.minutes
		currentChapter(emptyList(), 1.minutes) shouldBe null
	}
})
