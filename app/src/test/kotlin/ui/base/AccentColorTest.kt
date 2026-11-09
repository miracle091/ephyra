package org.jellyfin.androidtv.ui.base

import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.AccentColor
import org.jellyfin.androidtv.preference.constant.AppTheme

class AccentColorTest : FunSpec({
	// The real colours are Android resources; the palette logic only needs a distinct colour per resource id
	val resolve = { colorRes: Int -> Color(0xFF000000L or (colorRes.toLong() and 0xFFFFFFL)) }

	test("the theme default keeps the accent of every theme") {
		AppTheme.entries.forEach { theme ->
			val palette = paletteFor(theme)
			palette.withAccent(AccentColor.THEME, resolve) shouldBe palette
		}
	}

	test("a preset replaces only the accent tokens") {
		val palette = paletteFor(AppTheme.CORAL)
		val preset = AccentColor.AMBER
		val result = palette.withAccent(preset, resolve)

		result.accent shouldBe preset.accent?.let(resolve)
		result.onAccent shouldBe preset.onAccent?.let(resolve)
		result.accentSoft shouldBe preset.accentSoft?.let(resolve)
		result.copy(accent = palette.accent, onAccent = palette.onAccent, accentSoft = palette.accentSoft) shouldBe palette
	}

	test("every preset defines all its colours and a View overlay, the theme default none") {
		AccentColor.entries.forEach { entry ->
			if (entry == AccentColor.THEME) {
				entry.accent shouldBe null
				entry.overlayRes shouldBe null
			} else {
				(entry.accent != null && entry.onAccent != null && entry.accentSoft != null && entry.overlayRes != null) shouldBe true
			}
		}
	}
})
