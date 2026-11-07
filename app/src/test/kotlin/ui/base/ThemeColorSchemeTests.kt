package org.jellyfin.androidtv.ui.base

import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jellyfin.androidtv.preference.constant.AppTheme

class ThemeColorSchemeTests : FunSpec({
	test("the dark theme follows the design system") {
		val scheme = colorSchemeFor(AppTheme.DARK)

		scheme.background shouldBe Color(0xFF0A0F24)
		scheme.surface shouldBe Color(0xFF1B2547)
		scheme.buttonFocused shouldBe scheme.onBackground
		scheme.onButtonFocused shouldBe scheme.background
		scheme.rangeControlFill shouldBe Color(0xFF2BB5E8)
	}

	test("the black theme gives black backgrounds and near-black panels") {
		val scheme = colorSchemeFor(AppTheme.BLACK)

		scheme.background shouldBe Color.Black
		scheme.surface shouldBe Color(0xFF141414)
		scheme.listButtonFocused shouldNotBe scheme.surface
		scheme.rangeControlFill shouldBe colorSchemeFor(AppTheme.DARK).rangeControlFill
	}

	test("cover placeholders use each theme's surface-1") {
		paletteFor(AppTheme.DARK).surface1 shouldBe Color(0xFF121A36)
		paletteFor(AppTheme.BLACK).surface1 shouldBe Color(0xFF0A0A0A)
	}
})
