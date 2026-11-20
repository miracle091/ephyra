package org.jellyfin.androidtv.ui.player.base

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.playback.media3.exoplayer.SubtitleStyle

class SubtitleStylePreferenceTest : FunSpec({
	test("the user's subtitle settings become the players' subtitle style") {
		val preferences = mockk<UserPreferences> {
			every { this@mockk[UserPreferences.subtitlesTextColor] } returns 0xFFFFFF00
			every { this@mockk[UserPreferences.subtitlesBackgroundColor] } returns 0x80000000
			every { this@mockk[UserPreferences.subtitleTextStrokeColor] } returns 0x00FFFFFF
			every { this@mockk[UserPreferences.subtitlesTextWeight] } returns 700
			every { this@mockk[UserPreferences.subtitlesTextSize] } returns 32f
			every { this@mockk[UserPreferences.subtitlesOffsetPosition] } returns 0.12f
		}

		preferences.subtitleStyle() shouldBe SubtitleStyle(
			textColor = 0xFFFFFF00.toInt(),
			backgroundColor = 0x80000000.toInt(),
			strokeColor = 0x00FFFFFF,
			textWeight = 700,
			textSizeDp = 32f,
			bottomPaddingFraction = 0.12f,
		)
	}
})
