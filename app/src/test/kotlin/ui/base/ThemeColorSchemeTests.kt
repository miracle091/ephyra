package org.jellyfin.androidtv.ui.base

import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jellyfin.androidtv.preference.constant.AppTheme

class ThemeColorSchemeTests : FunSpec({
	test("the black theme gives black backgrounds and near-black panels") {
		val scheme = colorSchemeFor(AppTheme.BLACK)

		scheme.background shouldBe Color.Black
		scheme.surface shouldBe Color(0xFF141414)
		scheme.listButtonFocused shouldNotBe scheme.surface
	}

	test("the other themes keep upstream's colour scheme") {
		listOf(AppTheme.DARK, AppTheme.EMERALD, AppTheme.MUTED_PURPLE).forEach { theme ->
			colorSchemeFor(theme) shouldBe colorScheme()
		}
	}
})
