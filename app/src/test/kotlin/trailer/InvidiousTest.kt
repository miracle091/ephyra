package org.jellyfin.androidtv.trailer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.hours

class InvidiousTest : FunSpec({
	test("the public list keeps the HTTPS instances with the API on") {
		val json = """
			[
				["invidious.nerdvpn.de", {"type": "https", "uri": "https://invidious.nerdvpn.de", "api": false}],
				["yt.chocolatemoo53.com", {"type": "https", "uri": "https://yt.chocolatemoo53.com/", "api": true}],
				["example.onion", {"type": "onion", "uri": "http://example.onion", "api": true}],
				["broken", {"type": "https"}],
				["other.example", {"type": "https", "uri": "https://other.example", "api": null}]
			]
		""".trimIndent()

		parseInvidiousInstances(json) shouldBe listOf("https://yt.chocolatemoo53.com")
		parseInvidiousInstances("not json") shouldBe emptyList()
	}

	test("the user's instance goes first, then the last one that worked, then the list") {
		invidiousCandidates(
			own = "invidious.home.lan/",
			lastWorking = "https://b.example",
			listed = listOf("https://a.example", "https://b.example"),
		) shouldBe listOf("https://invidious.home.lan", "https://b.example", "https://a.example")
		invidiousCandidates(own = " ", lastWorking = "", listed = listOf("https://a.example")) shouldBe listOf("https://a.example")
		invidiousCandidates(own = "http://192.168.1.5:3000", lastWorking = null, listed = emptyList()) shouldBe
			listOf("http://192.168.1.5:3000")
	}

	test("the cached list is used for a day") {
		val now = 1_000_000_000_000L
		isInstanceListFresh(lastModified = now - 23.hours.inWholeMilliseconds, now = now) shouldBe true
		isInstanceListFresh(lastModified = now - 25.hours.inWholeMilliseconds, now = now) shouldBe false
		isInstanceListFresh(lastModified = 0L, now = now) shouldBe false
	}

	test("the trailer plays as the best AVC video up to 1080p with the default AAC audio") {
		val json = """
			{
				"adaptiveFormats": [
					{"itag": "140", "type": "audio/mp4; codecs=\"mp4a.40.2\"", "bitrate": "131003", "url": "https://i.example/a-dub",
						"audioTrack": {"audioIsDefault": false}},
					{"itag": "140", "type": "audio/mp4; codecs=\"mp4a.40.2\"", "bitrate": "130992", "url": "/videoplayback?a=140",
						"audioTrack": {"audioIsDefault": true}},
					{"itag": "251", "type": "audio/webm; codecs=\"opus\"", "bitrate": "143452", "url": "https://i.example/opus"},
					{"itag": "137", "type": "video/mp4; codecs=\"avc1.640028\"", "resolution": "1080p", "bitrate": "4000000",
						"url": "https://i.example/v1080"},
					{"itag": "264", "type": "video/mp4; codecs=\"avc1.640032\"", "resolution": "1440p", "url": "https://i.example/v1440"},
					{"itag": "303", "type": "video/webm; codecs=\"vp9\"", "resolution": "1080p", "url": "https://i.example/vp9"},
					{"itag": "136", "type": "video/mp4; codecs=\"avc1.4d401f\"", "resolution": "720p", "url": "https://i.example/v720"}
				],
				"formatStreams": [
					{"itag": "18", "type": "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"", "resolution": "360p", "url": "https://i.example/p360"}
				]
			}
		""".trimIndent()

		parseTrailerStreams(json, "https://i.example", "abc") shouldBe TrailerStreams(
			videoUrl = "https://i.example/latest_version?id=abc&itag=137&local=true",
			audioUrl = "https://i.example/latest_version?id=abc&itag=140&local=true",
		)
	}

	test("without separate AVC and AAC streams the progressive MP4 plays; nothing playable gives null") {
		val progressive = """
			{"adaptiveFormats": [], "formatStreams": [
				{"itag": "18", "type": "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"", "resolution": "360p", "url": "https://i.example/p360"},
				{"itag": "22", "type": "video/mp4; codecs=\"avc1.64001F, mp4a.40.2\"", "resolution": "720p", "url": "https://i.example/p720"}
			]}
		""".trimIndent()
		parseTrailerStreams(progressive, "https://i.example", "abc") shouldBe
			TrailerStreams("https://i.example/latest_version?id=abc&itag=22&local=true", null)

		parseTrailerStreams("""{"error": "This video is unavailable"}""", "https://i.example", "abc") shouldBe null
		val vp9Only = """{"adaptiveFormats": [{"itag": "303", "type": "video/webm; codecs=\"vp9\""}]}"""
		parseTrailerStreams(vp9Only, "https://i.example", "abc") shouldBe null
		parseTrailerStreams("<html>", "https://i.example", "abc") shouldBe null
	}

	test("the streams come through the instance's proxy, never from the addresses in its answer") {
		// Instances now hand out YouTube's own addresses even with local=true; /latest_version still proxies the stream
		val json = """
			{"adaptiveFormats": [], "formatStreams": [
				{"itag": "22", "type": "video/mp4; codecs=\"avc1.64001F, mp4a.40.2\"", "resolution": "720p",
					"url": "https://rr3---sn-abc.GoogleVideo.com/videoplayback?itag=22"}
			]}
		""".trimIndent()
		parseTrailerStreams(json, "https://i.example/", "a b") shouldBe
			TrailerStreams("https://i.example/latest_version?id=a%20b&itag=22&local=true", null)
		isYouTubeHost("www.youtube.com") shouldBe true
		isYouTubeHost("youtube.com.") shouldBe true
		isYouTubeHost("notyoutube.com") shouldBe false
		isYouTubeHost("i.example") shouldBe false
	}

	test("a malformed address or a stream without an itag is dropped instead of crashing the search") {
		invidiousCandidates(own = "invidious .home.lan", lastWorking = "https://host:abc", listed = listOf("https://a.example")) shouldBe
			listOf("https://a.example")
		parseTrailerStreams("""{"formatStreams": [{"itag": "18", "type": "video/mp4"}]}""", "http://[bad", "abc") shouldBe null
		parseTrailerStreams("""{"formatStreams": [{"type": "video/mp4", "url": "https://i.example/p"}]}""", "https://i.example", "abc") shouldBe
			null
		parseTrailerStreams("""{"formatStreams": [{"itag": "18&x=1", "type": "video/mp4"}]}""", "https://i.example", "abc") shouldBe null
	}
})
