package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.AccentColor
import org.jellyfin.androidtv.preference.constant.AppTheme

class ActivityThemeTest : FunSpec({
	test("a recreated activity gets the theme again although the view model remembers it") {
		// Velella: the view model outlives a configuration change, the new activity instance does not carry the theme
		themeStep(AppTheme.MEDUSA, AppTheme.MEDUSA, newInstance = true, AccentColor.TEAL, AccentColor.TEAL) shouldBe ThemeStep.APPLY
	}

	test("a running activity keeps its theme when nothing changed") {
		themeStep(AppTheme.MEDUSA, AppTheme.MEDUSA, newInstance = false, AccentColor.THEME, AccentColor.THEME) shouldBe ThemeStep.KEEP
	}

	test("the first start applies the theme") {
		themeStep(null, AppTheme.DARK, newInstance = true, appliedAccent = null, wantedAccent = AccentColor.THEME) shouldBe ThemeStep.APPLY
	}

	test("a theme changed while the activity runs recreates it") {
		themeStep(AppTheme.DARK, AppTheme.CORAL, newInstance = false, AccentColor.THEME, AccentColor.THEME) shouldBe ThemeStep.RECREATE
	}

	test("only the accent changed while the activity runs recreates it") {
		themeStep(AppTheme.DARK, AppTheme.DARK, newInstance = false, AccentColor.THEME, AccentColor.AMBER) shouldBe ThemeStep.RECREATE
		themeStep(AppTheme.DARK, AppTheme.DARK, newInstance = false, AccentColor.AMBER, AccentColor.THEME) shouldBe ThemeStep.RECREATE
	}
})
