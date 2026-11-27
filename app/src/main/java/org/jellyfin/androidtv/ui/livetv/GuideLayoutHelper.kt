package org.jellyfin.androidtv.ui.livetv

import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import java.time.LocalDateTime

/**
 * Velella: layout maths of the full-screen Live TV guide, kept out of the Java fragment.
 */
object GuideLayoutHelper {
	private const val HALF_HOUR = 30
	private const val MINUTE_MS = 60_000L
	private const val TICK_MARGIN_MS = 100L

	/** Where the guide starts for [now]: the half-hour mark [now] falls in (22:27 starts at 22:00, 22:41 at 22:30). */
	@JvmStatic
	fun guideStart(now: LocalDateTime): LocalDateTime =
		now.withMinute(now.minute - now.minute % HALF_HOUR).withSecond(0).withNano(0)

	/** Minutes from [start] to the next half-hour mark (0 when [start] sits on one). */
	@JvmStatic
	fun minutesToNextHalfHour(start: LocalDateTime): Int = (HALF_HOUR - start.minute % HALF_HOUR) % HALF_HOUR

	/** Where the half-hour filler [slot] (from 0) of a channel without programme data ends, in a guide starting at [guideStart]. */
	@JvmStatic
	fun fillerSlotEnd(guideStart: LocalDateTime, slot: Int): LocalDateTime = guideStart.plusMinutes(HALF_HOUR.toLong() * (slot + 1))

	/**
	 * Whether a guide page starting at [pageStart] has fallen behind [now]: a new half hour has begun, so the page showing the
	 * present moves on to it. A page the user moved to a later time is never behind.
	 */
	@JvmStatic
	fun isBehindNow(pageStart: LocalDateTime, now: LocalDateTime): Boolean = guideStart(now).isAfter(pageStart)

	/** Milliseconds from [nowMillis] to just after the next minute starts (a short margin so the clock has already turned). */
	@JvmStatic
	fun delayToNextMinute(nowMillis: Long): Long = MINUTE_MS - nowMillis % MINUTE_MS + TICK_MARGIN_MS

	/**
	 * Widths for cells laid side by side with [widths], so that each is at least [minPx] wide where the row allows it.
	 * A cell that grows takes the space from the cells after it (and, at the row's end, before it), so the row keeps its
	 * total width and the cells after the borrowed space line up with the time line again.
	 */
	@JvmStatic
	fun fitWidths(widths: IntArray, minPx: Int): IntArray {
		val count = widths.size
		if (count == 0) return widths
		val total = widths.sum()
		// Cell i ends at ends[i]; first push ends right so each cell gets minPx, then pull them back from the row's end.
		val ends = IntArray(count)
		var targetEnd = 0
		for (i in 0 until count) {
			targetEnd += widths[i]
			val previousEnd = if (i == 0) 0 else ends[i - 1]
			ends[i] = maxOf(targetEnd, previousEnd + minPx)
		}
		ends[count - 1] = total
		for (i in count - 2 downTo 0) ends[i] = minOf(ends[i], ends[i + 1] - minPx)
		for (i in 0 until count) ends[i] = ends[i].coerceIn(if (i == 0) 0 else ends[i - 1], total)
		return IntArray(count) { i -> ends[i] - (if (i == 0) 0 else ends[i - 1]) }
	}

	/** Applies [fitWidths] to the cells of [row], which must have fixed widths in their layout params. */
	@JvmStatic
	fun fitRowWidths(row: ViewGroup, minPx: Int) {
		val cells = (0 until row.childCount).map(row::getChildAt)
		val fitted = fitWidths(cells.map { it.layoutParams.width }.toIntArray(), minPx)
		cells.forEachIndexed { i, cell -> cell.layoutParams = cell.layoutParams.apply { width = fitted[i] } }
	}

	/**
	 * Scrolls [scroller] so the row of [content] that holds [focused] is fully visible with [marginPx] above and below.
	 * Does nothing when [focused] is not inside [content].
	 */
	@JvmStatic
	fun scrollRowIntoView(scroller: ScrollView, content: ViewGroup, focused: View?, marginPx: Int) {
		var row: View? = focused
		while (row != null && row.parent !== content) row = row.parent as? View
		if (row == null) return

		val top = row.top - marginPx
		val bottom = row.bottom + marginPx
		val visibleHeight = scroller.height - scroller.paddingTop - scroller.paddingBottom
		val scrollY = scroller.scrollY

		when {
			top < scrollY -> scroller.smoothScrollTo(0, top.coerceAtLeast(0))
			bottom > scrollY + visibleHeight -> scroller.smoothScrollTo(0, bottom - visibleHeight)
		}
	}
}
