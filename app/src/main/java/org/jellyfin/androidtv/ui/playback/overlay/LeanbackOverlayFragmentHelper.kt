package org.jellyfin.androidtv.ui.playback.overlay

import androidx.leanback.app.PlaybackSupportFragment
import androidx.leanback.app.RowsSupportFragment

/**
 * Ephyra: [PlaybackSupportFragment.onStart] (setVerticalGridViewLayout) sets the rows grid's window alignment offset to
 * -lb_playback_controls_padding_bottom, and the controls row (with the bottom shade) ends at that line, so a strip of video stayed
 * uncovered under the shade. The dimen can't be 0 (an offset of 0 counts from the top and moved the controls off screen), so call this
 * after onStart to put the line on the bottom edge: 100% of the grid's height, no offset.
 */
fun PlaybackSupportFragment.alignControlsToBottomEdge() {
	val rows = childFragmentManager.findFragmentById(androidx.leanback.R.id.playback_controls_dock) as? RowsSupportFragment
	val grid = rows?.verticalGridView ?: return
	grid.windowAlignmentOffset = 0
	grid.windowAlignmentOffsetPercent = 100f
}
