package org.jellyfin.androidtv.ui.player.video

import androidx.compose.ui.unit.IntSize
import org.jellyfin.androidtv.preference.constant.ZoomMode
import kotlin.math.roundToInt

/**
 * Ephyra: the size of the video picture in a [container] for the user's zoom mode, as the legacy player's resize modes:
 * [ZoomMode.FIT] shows the whole picture with black bars, [ZoomMode.AUTO_CROP] fills the screen keeping the shape (the
 * overflow is cut off), [ZoomMode.STRETCH] fills the screen ignoring the shape.
 */
fun videoFrameSize(container: IntSize, aspectRatio: Float, mode: ZoomMode): IntSize {
	if (container.width <= 0 || container.height <= 0 || aspectRatio.isNaN() || aspectRatio <= 0f) return container
	val containerRatio = container.width.toFloat() / container.height
	val matchWidth = when (mode) {
		ZoomMode.FIT -> aspectRatio >= containerRatio
		ZoomMode.AUTO_CROP -> aspectRatio < containerRatio
		ZoomMode.STRETCH -> return container
	}
	return when (matchWidth) {
		true -> IntSize(container.width, (container.width / aspectRatio).roundToInt())
		false -> IntSize((container.height * aspectRatio).roundToInt(), container.height)
	}
}
