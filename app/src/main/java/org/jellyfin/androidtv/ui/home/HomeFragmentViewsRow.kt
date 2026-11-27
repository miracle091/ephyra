package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import androidx.lifecycle.ProcessLifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.querying.GetUserViewsRequest
import org.jellyfin.androidtv.ui.composable.item.ROW_CARDS_WIDE
import org.jellyfin.androidtv.ui.composable.item.ROW_CARDS_WIDE_SMALL
import org.jellyfin.androidtv.ui.composable.item.WIDE_ASPECT_RATIO
import org.jellyfin.androidtv.ui.composable.item.rowCardHeight
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.apiclient.EmptyResponse

// Velella: the title before the libraries load, in the plural like most servers
private const val LIBRARIES_BEFORE_LOAD = 2

class HomeFragmentViewsRow(
	val small: Boolean,
) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		// Velella: a whole number of cards fills the row (upstream 75dp and 126dp)
		val screenWidth = context.resources.configuration.screenWidthDp.dp
		val count = if (small) ROW_CARDS_WIDE_SMALL else ROW_CARDS_WIDE
		val presenter = CardPresenter(true, rowCardHeight(screenWidth, count, WIDE_ASPECT_RATIO).value.toInt())
		val rowAdapter = ItemRowAdapter(context, GetUserViewsRequest, presenter, rowsAdapter)

		// Velella: "Libraries" (upstream "My media"), "Library" when there is only one
		fun header(libraries: Int) = HeaderItem(context.resources.getQuantityString(R.plurals.velella_home_libraries_row, libraries))
		val row = ListRow(header(LIBRARIES_BEFORE_LOAD), rowAdapter)
		@Suppress("DEPRECATION")
		rowAdapter.setRetrieveFinishedListener(object : EmptyResponse(ProcessLifecycleOwner.get().lifecycle) {
			override fun onResponse() {
				row.headerItem = header(rowAdapter.size())
				val position = rowsAdapter.indexOf(row)
				if (position >= 0) rowsAdapter.notifyItemRangeChanged(position, 1)
			}
		})
		rowAdapter.setRow(row)
		rowAdapter.Retrieve()
		rowsAdapter.add(row)
	}
}
