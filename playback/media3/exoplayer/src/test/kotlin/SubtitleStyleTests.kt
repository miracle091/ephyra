package org.jellyfin.playback.media3.exoplayer

import androidx.media3.ui.CaptionStyleCompat
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SubtitleStyleTests : FunSpec({
	test("an opaque stroke colour draws an outline") {
		subtitleEdgeType(0xFF000000.toInt()) shouldBe CaptionStyleCompat.EDGE_TYPE_OUTLINE
	}

	test("a half transparent stroke colour still draws an outline") {
		subtitleEdgeType(0x80FFFFFF.toInt()) shouldBe CaptionStyleCompat.EDGE_TYPE_OUTLINE
	}

	test("a fully transparent stroke colour draws no edge") {
		subtitleEdgeType(0x00FFFFFF) shouldBe CaptionStyleCompat.EDGE_TYPE_NONE
	}
})
