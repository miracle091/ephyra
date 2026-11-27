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

	/** Where the guide starts for [now]: the half-hour mark [now] falls in (22:27 starts at 22:00, 22:41 at 22:30). */
	@JvmStatic
	fun guideStart(now: LocalDateTime): LocalDateTime =
		now.withMinute(now.minute - now.minute % HALF_HOUR).withSecond(0).withNano(0)

	/** Minutes from [start] to the next half-hour mark (0 when [start] sits on one). */
	@JvmStatic
	fun minutesToNextHalfHour(start: LocalDateTime): Int = (HALF_HOUR - start.minute % HALF_HOUR) % HALF_HOUR

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
