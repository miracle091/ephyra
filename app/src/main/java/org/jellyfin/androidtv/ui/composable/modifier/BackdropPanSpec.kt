package org.jellyfin.androidtv.ui.composable.modifier

/**
 * Velella: the numbers of the slow backdrop pan ("Ken Burns") of the home spotlight and the long pause screen. Pure, so it can be
 * tested; [backdropPan] applies it.
 */
object BackdropPanSpec {
	/** One full pan, from rest to the end. A spotlight title shows for 10 s, so it only gets to the middle of it. */
	const val DURATION_MS = 24_000

	/** The scale at the end of the pan (starts at 1). */
	const val END_SCALE = 1.08f

	/** The shift at the end of the pan, as a fraction of the width and of the height. Inside the margin the scale leaves (4%). */
	const val SHIFT_X = 0.02f
	const val SHIFT_Y = 0.01f

	/** Whether to pan at all: not when the system's animations are off ("remove animations", animator duration scale 0). */
	fun isEnabled(animatorDurationScale: Float): Boolean = animatorDurationScale > 0f

	/** The side the pan moves to: 1 (right and down) for even [index], -1 (left and up) for odd, so neighbours alternate. */
	fun direction(index: Int): Int = if (Math.floorMod(index, 2) == 0) 1 else -1

	fun scale(progress: Float): Float = 1f + (END_SCALE - 1f) * progress.coerceIn(0f, 1f)

	/** Milliseconds left of the pan at [progress], used to carry on after a pause. */
	fun remainingMs(progress: Float): Int = ((1f - progress.coerceIn(0f, 1f)) * DURATION_MS).toInt()
}
