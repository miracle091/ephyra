package org.jellyfin.androidtv.integration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.UserPreferences

class TvProviderDefaultTest : FunSpec({
	// Ephyra: no launcher on the target Shield reads the channels, so the worker skips its server calls by default
	test("Home-screen channels and Watch Next are off by default") {
		UserPreferences.tvProviderEnabled.defaultValue shouldBe false
	}
})
