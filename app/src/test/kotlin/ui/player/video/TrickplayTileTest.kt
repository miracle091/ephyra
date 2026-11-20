package org.jellyfin.androidtv.ui.player.video

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.TrickplayInfoDto
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TrickplayTileTest : FunSpec({
	// 320x180 thumbnails every 10 s, 10x10 per tile image, 250 in all
	val info = TrickplayInfoDto(
		width = 320,
		height = 180,
		tileWidth = 10,
		tileHeight = 10,
		thumbnailCount = 250,
		interval = 10_000,
		bandwidth = 0,
	)

	test("the first thumbnail is the top left of the first tile image") {
		trickplayTile(0.seconds, info) shouldBe TrickplayTile(index = 0, x = 0, y = 0, width = 320, height = 180)
		trickplayTile(9_999.milliseconds, info) shouldBe TrickplayTile(index = 0, x = 0, y = 0, width = 320, height = 180)
	}

	test("thumbnails run left to right, then down the rows") {
		trickplayTile(30.seconds, info) shouldBe TrickplayTile(index = 0, x = 960, y = 0, width = 320, height = 180)
		trickplayTile(120.seconds, info) shouldBe TrickplayTile(index = 0, x = 640, y = 180, width = 320, height = 180)
	}

	test("past a full grid the next tile image starts") {
		trickplayTile(1_010.seconds, info) shouldBe TrickplayTile(index = 1, x = 320, y = 0, width = 320, height = 180)
	}

	test("tile images are prefetched from the one at the position, then after it, then before it") {
		trickplayTileOrder(info, 0.seconds) shouldBe listOf(0, 1, 2)
		trickplayTileOrder(info, 1_500.seconds) shouldBe listOf(1, 2, 0)
		trickplayTileOrder(info.copy(thumbnailCount = 0), 0.seconds) shouldBe emptyList()
	}

	test("a position past the last thumbnail keeps the last one") {
		trickplayTile(10_000.seconds, info) shouldBe TrickplayTile(index = 2, x = 2880, y = 720, width = 320, height = 180)
	}
})
