package org.jellyfin.androidtv.ui.playback.overlay.action

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PreferredLanguagesTest : FunSpec({
	// Track titles; the language is the part before the colon
	val tracks = listOf("eng:1", "ger:2", "ita:3", "fre:4", "ita:5", "none:6")
	fun List<String>.ordered(vararg preferred: String) = preferredLanguagesFirst(preferred.toList()) { it.substringBefore(':') }

	test("tracks in the preferred language come first, the others keep their order") {
		tracks.ordered("ita") shouldBe listOf("ita:3", "ita:5", "eng:1", "ger:2", "fre:4", "none:6")
	}

	test("several preferred languages keep their own order") {
		tracks.ordered("fre", "ita") shouldBe listOf("fre:4", "ita:3", "ita:5", "eng:1", "ger:2", "none:6")
	}

	test("no preferred language leaves the list as it is") {
		tracks.ordered() shouldBe tracks
		tracks.ordered("", " ") shouldBe tracks
	}

	test("ISO 639-2 bibliographic and terminology codes match each other, in any case") {
		tracks.ordered("DEU") shouldBe listOf("ger:2", "eng:1", "ita:3", "fre:4", "ita:5", "none:6")
		listOf("eng:1", "fra:2").ordered("fre") shouldBe listOf("fra:2", "eng:1")
	}

	test("two-letter codes match three-letter ones") {
		tracks.ordered("it") shouldBe listOf("ita:3", "ita:5", "eng:1", "ger:2", "fre:4", "none:6")
	}

	test("tracks without a language go after the preferred ones") {
		listOf<String?>(null, "ita").preferredLanguagesFirst(listOf("ita")) { it } shouldBe listOf("ita", null)
	}
})
