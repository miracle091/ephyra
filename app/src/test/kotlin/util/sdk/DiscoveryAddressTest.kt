package org.jellyfin.androidtv.util.sdk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DiscoveryAddressTest : FunSpec({
	val home = listOf(LocalNetwork("192.168.1.11", 24))

	test("a Docker container address is replaced by the address the reply came from, keeping scheme and port") {
		discoveredServerAddress("http://172.23.0.2:8096", "192.168.1.176", home) shouldBe "http://192.168.1.176:8096"
		discoveredServerAddress("https://172.23.0.2:8920/jellyfin", "192.168.1.176", home) shouldBe "https://192.168.1.176:8920/jellyfin"
	}

	test("an announced address on the device's network is kept") {
		discoveredServerAddress("http://192.168.1.50:8096", "192.168.1.176", home) shouldBe "http://192.168.1.50:8096"
		discoveredServerAddress("http://192.168.1.176:8096", "192.168.1.176", home) shouldBe "http://192.168.1.176:8096"
	}

	test("host names are kept, so HTTPS certificates still match") {
		discoveredServerAddress("https://jellyfin.example.com", "192.168.1.176", home) shouldBe "https://jellyfin.example.com"
	}

	test("nothing changes without a usable reply address or when the reply comes from another network") {
		discoveredServerAddress("http://172.23.0.2:8096", null, home) shouldBe "http://172.23.0.2:8096"
		discoveredServerAddress("http://172.23.0.2:8096", "fe80::1", home) shouldBe "http://172.23.0.2:8096"
		discoveredServerAddress("http://172.23.0.2:8096", "10.0.0.5", home) shouldBe "http://172.23.0.2:8096"
		discoveredServerAddress("http://172.23.0.2:8096", "192.168.1.176", emptyList()) shouldBe "http://172.23.0.2:8096"
	}

	test("a malformed announced address is kept as it is") {
		discoveredServerAddress("not a url", "192.168.1.176", home) shouldBe "not a url"
	}
})
