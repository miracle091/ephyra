package org.jellyfin.androidtv.ui.player.video

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamContainer
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.sdk.model.api.PlayMethod
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class VideoPlayerInfoTest : FunSpec({
	fun stream(method: MediaConversionMethod, range: String?) = PlayableMediaStream(
		identifier = "session",
		conversionMethod = method,
		container = MediaStreamContainer("mkv"),
		tracks = listOf(MediaStreamVideoTrack("hevc", 0, 3840, 2160, range)),
		queueEntry = QueueEntry(),
		url = "",
	)

	test("conversion methods map to the server's play methods") {
		MediaConversionMethod.None.playMethod shouldBe PlayMethod.DIRECT_PLAY
		MediaConversionMethod.Remux.playMethod shouldBe PlayMethod.DIRECT_STREAM
		MediaConversionMethod.Transcode.playMethod shouldBe PlayMethod.TRANSCODE
	}

	test("HDR10, HLG and Dolby Vision sent as they are dim the controls") {
		isHdrStream(stream(MediaConversionMethod.None, "HDR10")) shouldBe true
		isHdrStream(stream(MediaConversionMethod.Remux, "DOVI")) shouldBe true
		isHdrStream(stream(MediaConversionMethod.None, "HLG")) shouldBe true
	}

	test("SDR, unknown ranges, transcodes and no stream don't") {
		isHdrStream(stream(MediaConversionMethod.None, "SDR")) shouldBe false
		isHdrStream(stream(MediaConversionMethod.None, "Unknown")) shouldBe false
		isHdrStream(stream(MediaConversionMethod.None, null)) shouldBe false
		isHdrStream(stream(MediaConversionMethod.Transcode, "HDR10")) shouldBe false
		isHdrStream(null) shouldBe false
	}

	test("the info panel shows the audio track playing, not the first one") {
		val italian = MediaStreamAudioTrack("ac3", 0, 6, 48000, index = 1)
		val english = MediaStreamAudioTrack("eac3", 0, 6, 48000, index = 2)
		val source = stream(MediaConversionMethod.None, "SDR").copy(tracks = listOf(italian, english), defaultAudioStreamIndex = 1)

		playingAudioTrack(source, selectedIndex = 2) shouldBe english
		playingAudioTrack(source, selectedIndex = null) shouldBe italian
		playingAudioTrack(source.copy(defaultAudioStreamIndex = null), selectedIndex = 9) shouldBe italian
		playingAudioTrack(source.copy(tracks = emptyList()), selectedIndex = 1) shouldBe null
	}

	test("the end time counts the remaining time at the playback speed") {
		val now = LocalTime.of(20, 0)
		videoEndsAt(10.minutes, 40.minutes, 1f, now) shouldBe LocalTime.of(20, 30)
		videoEndsAt(10.minutes, 40.minutes, 1.5f, now) shouldBe LocalTime.of(20, 20)
		videoEndsAt(50.minutes, 40.minutes, 1f, now) shouldBe now
	}

	test("the remaining time is the media time left, with a minus sign") {
		remainingPlaybackTime(10.minutes, 40.minutes) shouldBe "-30:00"
		remainingPlaybackTime(25.seconds, 90.minutes) shouldBe "-1:29:35"
		remainingPlaybackTime(50.minutes, 40.minutes) shouldBe "-0:00"
	}

	test("the skip buttons show the skip length in whole seconds") {
		skipSecondsLabel(10.seconds) shouldBe "10"
		skipSecondsLabel(5_500.milliseconds) shouldBe "5"
		skipSecondsLabel(30.seconds) shouldBe "30"
	}
})
