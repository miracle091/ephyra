package org.jellyfin.androidtv.preference

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.constant.HomeSectionType

class HomeSectionDefaultsTest : FunSpec({
	// Velella: a lighter home by default; users who set their sections (here or in the web client) keep them
	test("the default home has libraries, continue watching, next up and latest, nothing else") {
		val defaults = with(UserSettingPreferences) {
			listOf(homesection0, homesection1, homesection2, homesection3, homesection4, homesection5, homesection6, homesection7)
		}.map { it.defaultValue }.filter { it != HomeSectionType.NONE }

		defaults shouldBe listOf(
			HomeSectionType.LIBRARY_TILES_SMALL,
			HomeSectionType.RESUME,
			HomeSectionType.NEXT_UP,
			HomeSectionType.LATEST_MEDIA,
		)
	}

	// Velella: the home has no On now row, the Live TV screen's spotlight shows the programmes on air
	test("live TV is no longer a home section, so a saved one falls back to the slot's default") {
		HomeSectionType.entries.map { it.serializedName } shouldNotContain "livetv"
	}
})
