package org.jellyfin.androidtv.data.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Velella: the screens sit on the theme's plain background; films, shows and music no longer paint their backdrops behind them (the user's
 * choice). What is left is the switch the video player uses to hide the app background while it plays.
 */
class BackgroundService {
	private val _enabled = MutableStateFlow(true)
	val enabled get() = _enabled.asStateFlow()

	/** Show the app background again, after [disable]. */
	fun clearBackgrounds() {
		_enabled.value = true
	}

	/**
	 * Disable the showing of backgrounds until any function manipulating the backgrounds is called.
	 */
	fun disable() {
		_enabled.value = false
	}
}
