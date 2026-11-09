package org.jellyfin.playback.jellyfin.mediastream

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BluRayTransportStreamTests : FunSpec({
	test("Blu-ray transport streams are recognised by their extension") {
		isBluRayTransportStream("/data/Film/test/vc1-with-truehd.m2ts") shouldBe true
		isBluRayTransportStream("/data/BDMV/STREAM/00001.M2TS") shouldBe true
		isBluRayTransportStream("/data/camera/clip.mts") shouldBe true
	}

	test("plain transport streams and other files are not") {
		isBluRayTransportStream("/data/tv/recording.ts") shouldBe false
		isBluRayTransportStream("/data/Film/movie.mkv") shouldBe false
		isBluRayTransportStream("/data/Film/m2ts.mkv") shouldBe false
		isBluRayTransportStream(null) shouldBe false
	}
})
