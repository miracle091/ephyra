package org.jellyfin.androidtv.ui.playback

import android.widget.TextView
import androidx.core.view.isVisible
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.getTimeFormatter
import org.jellyfin.sdk.model.api.BaseItemDto
import java.time.LocalDateTime

/**
 * Velella: the "Next: title · time" line under the programme on air in the live TV player's header, after the design system's
 * "Live TV" board. The line is hidden when the channel has no programme after the one on air.
 */
object NextProgram {
	/**
	 * The programme that follows the one ending at [currentEnd]: the earliest of [programs] that starts at or after it and has a
	 * title and a start time. Null when [currentEnd] is unknown or nothing follows.
	 */
	@JvmStatic
	fun pick(programs: Collection<BaseItemDto>?, currentEnd: LocalDateTime?): BaseItemDto? {
		if (programs == null || currentEnd == null) return null

		return programs
			.filter { !it.name.isNullOrBlank() }
			.filter { program -> program.startDate?.let { !it.isBefore(currentEnd) } == true }
			.minByOrNull { requireNotNull(it.startDate) }
	}

	@JvmStatic
	fun bind(view: TextView, next: BaseItemDto?) {
		val start = next?.startDate
		if (next == null || start == null) {
			view.isVisible = false
			return
		}

		view.text = view.context.getString(R.string.velella_osd_next_program, next.name, view.context.getTimeFormatter().format(start))
		view.isVisible = true
	}
}
