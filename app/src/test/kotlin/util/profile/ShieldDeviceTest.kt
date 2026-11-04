package org.jellyfin.androidtv.util.profile

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ShieldDeviceTest : FunSpec({
	test("Every Shield TV model reports the same manufacturer and model") {
		ShieldDevice.isShieldTv("NVIDIA", "SHIELD Android TV") shouldBe true
		ShieldDevice.isShieldTv("nvidia", "SHIELD Android TV") shouldBe true
	}

	test("Other devices are not a Shield TV") {
		ShieldDevice.isShieldTv("Google", "Chromecast") shouldBe false
		ShieldDevice.isShieldTv("NVIDIA", "SHIELD Tablet K1") shouldBe false
		ShieldDevice.isShieldTv("Google", "sdk_google_atv64_amati_arm64") shouldBe false
	}
})
