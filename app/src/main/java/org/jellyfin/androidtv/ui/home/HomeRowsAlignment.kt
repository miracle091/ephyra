package org.jellyfin.androidtv.ui.home

import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import org.jellyfin.androidtv.util.dp

// Where the cards of the selected row start, from the top of the rows: room for its title and the end of the row above
private const val HOME_ROW_KEYLINE_DP = 118

/**
 * Ephyra: the selected home row's cards start at a fixed line from the top, instead of being centred. When the spotlight makes
 * the rows short, centring scrolled the first row's title out of sight; the low edge keeps the first row where it starts.
 */
fun VerticalGridView.alignHomeRows() {
	itemAlignmentOffset = 0
	itemAlignmentOffsetPercent = 0f
	windowAlignment = BaseGridView.WINDOW_ALIGN_BOTH_EDGE
	windowAlignmentOffsetPercent = BaseGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
	windowAlignmentOffset = HOME_ROW_KEYLINE_DP.dp(context)
	isWindowAlignmentPreferKeyLineOverLowEdge = false
}
