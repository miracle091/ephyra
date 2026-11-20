package org.jellyfin.androidtv.ui.player.display

import android.app.Activity
import android.view.Display
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior
import timber.log.Timber

private fun Display.Mode.toInfo() = DisplayModeInfo(modeId, DisplaySize(physicalWidth, physicalHeight), refreshRate)

/**
 * Ephyra: asks the display for the mode that fits [video] (see [findBestDisplayMode]), shared by both players. Does nothing when
 * switching is disabled, no mode fits or the display is already in the best one. Undo it with [resetDisplayMode] on exit.
 */
fun Activity.switchDisplayMode(video: VideoModeRequest, behavior: RefreshRateSwitchingBehavior) {
	if (behavior == RefreshRateSwitchingBehavior.DISABLED) return
	val display = display ?: return

	val current = display.mode.toInfo()
	val modes = display.supportedModes.map { it.toInfo() }
	Timber.d("Display modes: $modes, current ${current.id}")

	val best = findBestDisplayMode(modes, current, video, behavior)
	when {
		best == null -> Timber.i("No display mode for $video")
		best.id == current.id -> Timber.i("Display is already in the best mode for $video")
		else -> {
			Timber.i("Switching display mode from $current to $best for $video")
			window.attributes = window.attributes.apply { preferredDisplayModeId = best.id }
		}
	}
}

/** Ephyra: hands the display mode back to the system (the mode it had before playback). */
fun Activity.resetDisplayMode() {
	window.attributes = window.attributes.apply { preferredDisplayModeId = 0 }
}
