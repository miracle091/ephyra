package org.jellyfin.androidtv.ui.player.base.toast

import androidx.annotation.DrawableRes
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MediaToastRegistry(
	private val coroutineScope: CoroutineScope,
) {
	companion object {
		val TOAST_DURATION = 700.milliseconds
	}

	private val _current = MutableStateFlow<MediaToastData?>(null)
	val current = _current.asStateFlow()

	private var unsetJob: Job? = null

	fun emit(data: MediaToastData) {
		if (current.value == data) return

		_current.value = data

		unsetJob?.cancel()
		unsetJob = coroutineScope.launch {
			delay(TOAST_DURATION)
			_current.value = null
		}
	}

	fun emit(
		@DrawableRes icon: Int,
		progress: Float? = null,
		label: String? = null,
	) {
		val data = MediaToastData(
			icon = icon,
			progress = progress,
			label = label,
		)
		emit(data)
	}
}

/** Velella: the player's [MediaToastRegistry] for its controls, so the play/pause button shows the icon of the user's command. */
val LocalMediaToastRegistry = staticCompositionLocalOf<MediaToastRegistry> { error("No MediaToastRegistry provided") }

