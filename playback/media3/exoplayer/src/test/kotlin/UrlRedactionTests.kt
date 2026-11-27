package org.jellyfin.playback.media3.exoplayer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UrlRedactionTests : FunSpec({
	test("redacts ApiKey and keeps the rest of the URL") {
		"https://tv.example/videos/1/master.m3u8?MediaSourceId=abc&ApiKey=secret123&VideoCodec=h264"
			.redactApiKey() shouldBe "https://tv.example/videos/1/master.m3u8?MediaSourceId=abc&ApiKey=***&VideoCodec=h264"
	}

	test("redacts api_key in any case, first or last parameter") {
		"https://tv.example/a?api_key=secret".redactApiKey() shouldBe "https://tv.example/a?api_key=***"
		"https://tv.example/a?x=1&API_KEY=secret".redactApiKey() shouldBe "https://tv.example/a?x=1&API_KEY=***"
	}

	test("redacts every occurrence and stops at a fragment") {
		"https://tv.example/a?ApiKey=one&b=2&api_key=two#frag".redactApiKey() shouldBe
			"https://tv.example/a?ApiKey=***&b=2&api_key=***#frag"
	}

	test("leaves URLs without a key and look-alike parameters untouched") {
		"https://tv.example/a?x=1&y=2".redactApiKey() shouldBe "https://tv.example/a?x=1&y=2"
		"https://tv.example/a?myapi_key=1&ApiKeyHint=2".redactApiKey() shouldBe "https://tv.example/a?myapi_key=1&ApiKeyHint=2"
	}

	test("a server URL is logged without its token, a remote source only by its host") {
		"https://tv.example/Videos/1/stream?static=true&api_key=abc".redactForLog() shouldBe
			"https://tv.example/Videos/1/stream?static=true&api_key=***"
		"http://iptv.example:8080/live/user/pass/1.ts".redactForLog() shouldBe "http://iptv.example:8080"
		"https://cdn.example/movie.mp4?token=secret".redactForLog() shouldBe "https://cdn.example"
		"not a url".redactForLog() shouldBe "<stream>"
	}
})
