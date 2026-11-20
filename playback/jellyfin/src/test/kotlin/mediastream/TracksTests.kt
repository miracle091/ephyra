package org.jellyfin.playback.jellyfin.mediastream

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.playback.core.mediastream.MediaStreamSubtitleTrack
import org.jellyfin.sdk.model.api.AudioSpatialFormat
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.VideoRange
import org.jellyfin.sdk.model.api.VideoRangeType

private fun subtitle(index: Int, deliveryMethod: SubtitleDeliveryMethod?, deliveryUrl: String?, isExternal: Boolean = false) = MediaStream(
	type = MediaStreamType.SUBTITLE,
	index = index,
	codec = "subrip",
	language = "ita",
	displayTitle = "Italiano - SUBRIP",
	isDefault = false,
	isForced = true,
	isExternal = isExternal,
	deliveryMethod = deliveryMethod,
	deliveryUrl = deliveryUrl,
	isInterlaced = false,
	isAvc = false,
	isTextSubtitleStream = true,
	supportsExternalStream = true,
	isHearingImpaired = false,
	videoRange = VideoRange.UNKNOWN,
	videoRangeType = VideoRangeType.UNKNOWN,
	audioSpatialFormat = AudioSpatialFormat.NONE,
	isOriginal = false,
)

class TracksTests : FunSpec({
	val createUrl = { path: String -> "https://server$path" }

	test("a subtitle the server hands out as a file gets its full URL") {
		subtitle(3, SubtitleDeliveryMethod.EXTERNAL, "/Videos/1/1/Subtitles/3/0/Stream.srt", isExternal = true)
			.getMediaStreamTrack(createUrl) shouldBe MediaStreamSubtitleTrack(
			codec = "subrip",
			index = 3,
			language = "ita",
			title = "Italiano - SUBRIP",
			isDefault = false,
			isForced = true,
			isExternal = true,
			externalUrl = "https://server/Videos/1/1/Subtitles/3/0/Stream.srt",
		)
	}

	test("an embedded or burnt-in subtitle comes with the stream, so it has no URL") {
		(subtitle(2, SubtitleDeliveryMethod.EMBED, null).getMediaStreamTrack(createUrl) as MediaStreamSubtitleTrack).externalUrl shouldBe null
		(subtitle(2, SubtitleDeliveryMethod.ENCODE, null).getMediaStreamTrack(createUrl) as MediaStreamSubtitleTrack).externalUrl shouldBe null
	}

	test("a subtitle burnt in while transcoding loses its file, the others keep theirs") {
		val tracks = listOf(
			subtitle(3, SubtitleDeliveryMethod.EXTERNAL, "/a.srt").getMediaStreamTrack(createUrl),
			subtitle(4, SubtitleDeliveryMethod.EXTERNAL, "/b.srt").getMediaStreamTrack(createUrl),
		).filterIsInstance<MediaStreamSubtitleTrack>()

		val result = tracks.withBurntInSubtitle(3).filterIsInstance<MediaStreamSubtitleTrack>()

		result.map { it.externalUrl } shouldBe listOf(null, "https://server/b.srt")
	}

	test("a subtitle in the HLS manifest is not loaded a second time") {
		val track = subtitle(4, SubtitleDeliveryMethod.HLS, "/Videos/1/1/Subtitles/4/subtitles.m3u8").getMediaStreamTrack(createUrl)
		(track as MediaStreamSubtitleTrack).externalUrl shouldBe null
	}
})
