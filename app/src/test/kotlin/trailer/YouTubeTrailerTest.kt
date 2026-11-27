package org.jellyfin.androidtv.trailer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaUrl
import java.util.UUID

class YouTubeTrailerTest : FunSpec({
	test("the video id comes from the YouTube link shapes the server stores") {
		youTubeVideoId("https://www.youtube.com/watch?v=aqz-KE-bpKQ") shouldBe "aqz-KE-bpKQ"
		youTubeVideoId("http://youtube.com/watch?feature=share&v=aqz-KE-bpKQ") shouldBe "aqz-KE-bpKQ"
		youTubeVideoId("https://m.youtube.com/watch?v=aqz-KE-bpKQ&t=10") shouldBe "aqz-KE-bpKQ"
		youTubeVideoId("https://youtu.be/aqz-KE-bpKQ?si=x") shouldBe "aqz-KE-bpKQ"
		youTubeVideoId("https://www.youtube.com/embed/aqz-KE-bpKQ") shouldBe "aqz-KE-bpKQ"
		youTubeVideoId("https://www.youtube-nocookie.com/embed/aqz-KE-bpKQ") shouldBe "aqz-KE-bpKQ"
		youTubeVideoId(" https://www.youtube.com/shorts/aqz-KE-bpKQ ") shouldBe "aqz-KE-bpKQ"
	}

	test("other sites, other YouTube pages and malformed ids give no id") {
		youTubeVideoId("https://vimeo.com/123456") shouldBe null
		youTubeVideoId("https://www.youtube.com/channel/UC123") shouldBe null
		youTubeVideoId("https://www.youtube.com/watch?v=short") shouldBe null
		youTubeVideoId("https://notyoutube.com/watch?v=aqz-KE-bpKQ") shouldBe null
		youTubeVideoId("not a url") shouldBe null
	}

	test("an item's trailers are its YouTube ids, without repeats, ignoring other sites") {
		val item = BaseItemDto(
			id = UUID.randomUUID(),
			type = BaseItemKind.MOVIE,
			remoteTrailers = listOf(
				MediaUrl(url = "https://vimeo.com/123456"),
				MediaUrl(url = "https://www.youtube.com/watch?v=aqz-KE-bpKQ"),
				MediaUrl(url = "https://youtu.be/aqz-KE-bpKQ"),
				MediaUrl(url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
				MediaUrl(url = null),
			),
		)
		item.youTubeTrailerIds() shouldBe listOf("aqz-KE-bpKQ", "dQw4w9WgXcQ")
	}
})
