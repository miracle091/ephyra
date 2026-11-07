package org.jellyfin.androidtv.ui.base

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.AppTheme
import java.io.File

private fun contrast(a: Color, b: Color): Double {
	val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
	return (light + 0.05) / (dark + 0.05)
}

private fun Color.hex() = "#%06X".format(value.shr(32).toLong() and 0xFFFFFF)

class ThemePaletteTests : FunSpec({
	AppTheme.entries.forEach { theme ->
		test("$theme keeps text, accent and focus readable") {
			val palette = paletteFor(theme)

			contrast(palette.ink, palette.bg) shouldBeGreaterThanOrEqual 7.0
			contrast(palette.inkMuted, palette.surface2) shouldBeGreaterThanOrEqual 4.5
			contrast(palette.inkFaint, palette.bg) shouldBeGreaterThanOrEqual 4.5
			contrast(palette.accent, palette.bg) shouldBeGreaterThanOrEqual 4.5
			contrast(palette.onAccent, palette.accent) shouldBeGreaterThanOrEqual 4.5
			// Focus is an ink fill with bg content
			contrast(palette.bg, palette.ink) shouldBeGreaterThanOrEqual 7.0
		}
	}

	mapOf(
		AppTheme.ABYSS to "abyss",
		AppTheme.CORAL to "coral",
		AppTheme.MEDUSA to "medusa",
	).forEach { (theme, name) ->
		test("the Leanback theme of $theme uses the same colours") {
			val xml = File("src/main/res/values/theme_ephyra_$name.xml").readText().uppercase()
			val palette = paletteFor(theme)

			listOf(palette.bg, palette.surface1, palette.surface2, palette.surface3, palette.accent, palette.inkMuted, palette.inkFaint)
				.map { ">${it.hex()}<" }
				.filterNot { it in xml } shouldBe emptyList()
			("THEME.JELLYFIN.EPHYRA.${name.uppercase()}" in xml) shouldBe true
		}
	}
})

