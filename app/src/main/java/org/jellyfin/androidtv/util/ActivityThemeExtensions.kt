package org.jellyfin.androidtv.util

import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AccentColor
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Getter to get the style resource for a given theme.
 */
private val AppTheme.style
	get() = when (this) {
		AppTheme.DARK -> R.style.Theme_Jellyfin_Velella // Velella
		AppTheme.BLACK -> R.style.Theme_Jellyfin_Black // Velella
		AppTheme.ABYSS -> R.style.Theme_Jellyfin_Velella_Abyss // Velella
		AppTheme.CORAL -> R.style.Theme_Jellyfin_Velella_Coral // Velella
		AppTheme.MEDUSA -> R.style.Theme_Jellyfin_Velella_Medusa // Velella
	}

/**
 * Private view model for the [applyTheme] extension to store the currently set theme.
 */
class ThemeViewModel : ViewModel() {
	var theme: AppTheme? = null
	var accent: AccentColor? = null // Velella
}

/**
 * Extension function to set the theme. Should be called in [FragmentActivity.onCreate] and
 * [FragmentActivity.onResume]. It recreates the activity when the theme changed after it was set.
 * Do not call during resume if the activity may not be recreated (like in the video player).
 */
fun FragmentActivity.applyTheme() {
	val viewModel by viewModels<ThemeViewModel>()
	val userPreferences by inject<UserPreferences>()
	val velellaPreferences by inject<VelellaPreferences>()
	val theme = userPreferences[UserPreferences.appTheme]
	val accent = velellaPreferences[VelellaPreferences.accentColor] // Velella

	// Velella: before onCreate the instance has no theme yet, even when the retained view model says it was applied
	val newInstance = lifecycle.currentState == Lifecycle.State.INITIALIZED

	val step = themeStep(
		applied = viewModel.theme,
		wanted = theme,
		newInstance = newInstance,
		appliedAccent = viewModel.accent,
		wantedAccent = accent,
	)

	when (step) {
		ThemeStep.KEEP -> Unit
		ThemeStep.RECREATE -> {
			Timber.i("Recreating activity to apply theme")
			viewModel.theme = null
			viewModel.accent = null
			recreate()
		}
		ThemeStep.APPLY -> {
			Timber.i("Applying theme $theme with accent $accent")
			viewModel.theme = theme
			viewModel.accent = accent
			setTheme(theme.style)
			// Velella: the accent overlay goes over the theme and replaces only the attributes that carry the accent
			accent.overlayRes?.let { getTheme().applyStyle(it, true) }
		}
	}
}

/** Velella: what [applyTheme] does for the theme [applied] to the view model, the [wanted] one and a not yet created activity. */
enum class ThemeStep { KEEP, APPLY, RECREATE }

/**
 * Velella: a changed theme or accent recreates a running activity; a new instance, or none applied yet, gets both applied.
 */
fun themeStep(
	applied: AppTheme?,
	wanted: AppTheme,
	newInstance: Boolean,
	appliedAccent: AccentColor?,
	wantedAccent: AccentColor,
): ThemeStep = when {
	applied == null -> ThemeStep.APPLY
	applied != wanted || appliedAccent != wantedAccent -> ThemeStep.RECREATE
	newInstance -> ThemeStep.APPLY
	else -> ThemeStep.KEEP
}
