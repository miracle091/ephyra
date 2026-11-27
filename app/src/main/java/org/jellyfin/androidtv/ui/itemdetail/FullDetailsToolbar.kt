@file:JvmName("FullDetailsFragmentHelperKt")
@file:JvmMultifileClass

package org.jellyfin.androidtv.ui.itemdetail

import android.view.View
import org.jellyfin.androidtv.R

private const val TOOLBAR_FADE_MS = 150L

/**
 * Velella: the home button, clock and now playing bug are drawn above the rows; fade them out unless the details overview
 * (the first row) is selected, so they never sit on top of the cards that scroll up under them.
 */
fun FullDetailsFragment.updateToolbarVisibility(overviewSelected: Boolean) {
	val root = view ?: return
	val alpha = if (overviewSelected) 1f else 0f

	for (id in intArrayOf(R.id.fdClock, R.id.npBug)) {
		root.findViewById<View>(id)?.animate()?.alpha(alpha)?.setDuration(TOOLBAR_FADE_MS)?.start()
	}
}
