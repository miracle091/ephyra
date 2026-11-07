package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.querying.GetUserViewsRequest
import org.jellyfin.androidtv.ui.composable.item.ROW_CARDS_WIDE
import org.jellyfin.androidtv.ui.composable.item.ROW_CARDS_WIDE_SMALL
import org.jellyfin.androidtv.ui.composable.item.WIDE_ASPECT_RATIO
import org.jellyfin.androidtv.ui.composable.item.rowCardHeight
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter

class HomeFragmentViewsRow(
	val small: Boolean,
) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		// Ephyra: a whole number of cards fills the row (upstream 75dp and 126dp)
		val screenWidth = context.resources.configuration.screenWidthDp.dp
		val count = if (small) ROW_CARDS_WIDE_SMALL else ROW_CARDS_WIDE
		val presenter = CardPresenter(true, rowCardHeight(screenWidth, count, WIDE_ASPECT_RATIO).value.toInt())
		val rowAdapter = ItemRowAdapter(context, GetUserViewsRequest, presenter, rowsAdapter)

		val header = HeaderItem(context.getString(R.string.lbl_my_media))
		val row = ListRow(header, rowAdapter)
		rowAdapter.setRow(row)
		rowAdapter.Retrieve()
		rowsAdapter.add(row)
	}
}
