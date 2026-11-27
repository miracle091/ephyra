package org.jellyfin.androidtv.preference

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.constant.HomeSectionType

class HomeSectionDefaultsTest : FunSpec({
	// Velella: a lighter home by default; users who set their sections (here or in the web client) keep them
	test("the default home has libraries, continue watching, live TV, next up and latest, nothing else") {
		val defaults = with(UserSettingPreferences) {
			listOf(homesection0, homesection1, homesection2, homesection3, homesection4, homesection5, homesection6, homesection7)
		}.map { it.defaultValue }.filter { it != HomeSectionType.NONE }

		defaults shouldBe listOf(
			HomeSectionType.LIBRARY_TILES_SMALL,
			HomeSectionType.RESUME,
			HomeSectionType.LIVE_TV,
			HomeSectionType.NEXT_UP,
			HomeSectionType.LATEST_MEDIA,
		)
	}
})
