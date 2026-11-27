package org.jellyfin.androidtv.ui.livetv

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.jellyfin.androidtv.R
import java.time.Duration
import java.time.LocalDateTime

/**
 * Velella: the look of the Live TV guide after the design system, shared by the grid cells, the channel headers and the paging
 * buttons.
 */
object GuideStyle {
	private const val CELL_RADIUS_DP = 12f
	private const val CELL_GAP_DP = 2
	private const val NOW_BAR_MAX = 1000

	/** A guide cell background: surface-2 with radius-md, surface-3 and an ink ring on focus, [tint] over it for colour coding. */
	@JvmStatic
	@JvmOverloads
	fun cellBackground(context: Context, tint: Int = 0): Drawable {
		val base = requireNotNull(ContextCompat.getDrawable(context, R.drawable.velella_guide_cell)) { "Missing guide cell drawable" }
		if (tint == 0) return base

		val density = context.resources.displayMetrics.density
		val tintLayer = GradientDrawable().apply {
			setColor(tint)
			cornerRadius = CELL_RADIUS_DP * density
		}
		val gap = (CELL_GAP_DP * density).toInt()
		return LayerDrawable(arrayOf(base, InsetDrawable(tintLayer, gap)))
	}

	/** Times in the guide use the mono font, so the digits line up. */
	@JvmStatic
	fun monoTypeface(context: Context): Typeface? = ResourcesCompat.getFont(context, R.font.atkinson_hyperlegible_mono)

	/**
	 * How far the programme on air has run, from 0 to [NOW_BAR_MAX], over its whole length, also when it began before the
	 * guide's start. Returns -1 when the programme is not on air at [now].
	 */
	@JvmStatic
	fun nowProgress(start: LocalDateTime, end: LocalDateTime, now: LocalDateTime): Int {
		if (now.isBefore(start) || !now.isBefore(end)) return -1

		val total = Duration.between(start, end).toMillis()
		if (total <= 0L) return -1

		val elapsed = Duration.between(start, now).toMillis()
		return (elapsed * NOW_BAR_MAX / total).toInt().coerceIn(0, NOW_BAR_MAX)
	}
}
