package org.jellyfin.androidtv.preference

import android.content.Context
import androidx.preference.PreferenceManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.jellyfin.androidtv.update.MapSharedPreferences

class PlayerDefaultTest : FunSpec({
	val context = mockk<Context>()

	fun userPreferences(prefs: MapSharedPreferences): UserPreferences {
		every { PreferenceManager.getDefaultSharedPreferences(context) } returns prefs
		return UserPreferences(context)
	}

	beforeSpec { mockkStatic(PreferenceManager::class) }
	afterSpec { unmockkStatic(PreferenceManager::class) }

	// Velella: legacy-player-drop phase 6, the new video player is the default for everyone
	test("A fresh install plays video with the new player") {
		userPreferences(MapSharedPreferences())[UserPreferences.playbackRewriteVideoEnabled] shouldBe true
	}

	test("An update moves a user who had the old player to the new one") {
		val prefs = MapSharedPreferences().apply {
			values["store_version"] = 9
			values["playback_new"] = false
		}

		userPreferences(prefs)[UserPreferences.playbackRewriteVideoEnabled] shouldBe true
	}

	test("Picking the old player as a fallback after the update sticks") {
		val prefs = MapSharedPreferences().apply { values["store_version"] = 9 }
		userPreferences(prefs)[UserPreferences.playbackRewriteVideoEnabled] = false

		userPreferences(prefs)[UserPreferences.playbackRewriteVideoEnabled] shouldBe false
	}
})
