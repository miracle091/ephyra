package org.jellyfin.playback.jellyfin.mediastream

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo

private fun source(
	id: String,
	protocol: MediaProtocol = MediaProtocol.FILE,
	isRemote: Boolean = false,
	path: String? = "/media/a.mkv",
	supportsDirectPlay: Boolean = true,
) = mockk<MediaSourceInfo> {
	every { this@mockk.id } returns id
	every { this@mockk.protocol } returns protocol
	every { this@mockk.isRemote } returns isRemote
	every { this@mockk.path } returns path
	every { this@mockk.supportsDirectPlay } returns supportsDirectPlay
}

class LiveStreamTests : FunSpec({
	test("a live channel's or a strm file's remote source is kept, not filtered out as before") {
		val live = source("live", MediaProtocol.HTTP, isRemote = true, path = "http://tuner/1")
		selectMediaSource(listOf(live), null) shouldBe live
	}

	test("the requested source is picked, else the first") {
		val a = source("a")
		val b = source("b")
		selectMediaSource(listOf(a, b), "b") shouldBe b
		selectMediaSource(listOf(a, b), null) shouldBe a
		selectMediaSource(listOf(a, b), "c") shouldBe null
	}

	test("only file and HTTP sources play directly; the server converts the others") {
		source("a").canDirectPlay shouldBe true
		source("a", MediaProtocol.HTTP, isRemote = true).canDirectPlay shouldBe true
		source("a", MediaProtocol.RTSP, isRemote = true).canDirectPlay shouldBe false
		source("a", supportsDirectPlay = false).canDirectPlay shouldBe false
	}

	test("a remote HTTP source plays from its own address, a local one through the server") {
		source("a", MediaProtocol.HTTP, isRemote = true, path = "http://tuner/1").remoteDirectUrl shouldBe "http://tuner/1"
		source("a").remoteDirectUrl shouldBe null
		source("a", MediaProtocol.HTTP, isRemote = true, path = " ").remoteDirectUrl shouldBe null
	}

	test("only TV channels count as live") {
		mockk<BaseItemDto> { every { type } returns BaseItemKind.TV_CHANNEL }.isLiveTvChannel shouldBe true
		mockk<BaseItemDto> { every { type } returns BaseItemKind.MOVIE }.isLiveTvChannel shouldBe false
	}
})
