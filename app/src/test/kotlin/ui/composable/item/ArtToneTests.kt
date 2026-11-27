package org.jellyfin.androidtv.ui.composable.item

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.jellyfin.design.token.VelellaColorTokens

class ArtToneTests : FunSpec({
	test("a title always gets the same art tone") {
		artToneFor("Dune: Part Two") shouldBe artToneFor("Dune: Part Two")
	}

	test("missing titles and negative hashes still get a tone") {
		// "polygenelubricants" hashes to Int.MIN_VALUE
		listOf(null, "", "Polly", "polygenelubricants").forEach { title ->
			artToneFor(title) shouldBeIn VelellaColorTokens.art
		}
	}

	test("different titles spread over the tones") {
		val titles = listOf("Movies", "Music", "Playlists", "Shows", "Collections", "Live TV", "Books", "Photos", "Home videos")
		titles.map(::artToneFor).toSet().size shouldBeGreaterThan 3
	}
})
