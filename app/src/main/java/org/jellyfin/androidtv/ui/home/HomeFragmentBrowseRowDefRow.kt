package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import androidx.lifecycle.ProcessLifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.data.querying.GetUserViewsRequest
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.GridButtonBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.apiclient.EmptyResponse
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val LIVE_TV_CHANNEL_CHUNK_SIZE = 40

class HomeFragmentBrowseRowDefRow(
	private val browseRowDef: BrowseRowDef,
	/** Velella: the library a "See all" card at the end of the row opens, when the row has one. */
	private val seeAll: BaseItemDto? = null,
) : HomeFragmentRow, KoinComponent {
	private val userPreferences by inject<UserPreferences>()

	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val header = HeaderItem(browseRowDef.headerText)
		val preferParentThumb = userPreferences[UserPreferences.seriesThumbnailsEnabled]

		// Some of these members are probably never used and could be removed
		val rowAdapter = when (browseRowDef.queryType) {
			QueryType.NextUp -> ItemRowAdapter(context, browseRowDef.nextUpQuery, preferParentThumb, cardPresenter, rowsAdapter)
			QueryType.LatestItems -> ItemRowAdapter(
				context,
				browseRowDef.latestItemsQuery,
				userPreferences[UserPreferences.seriesThumbnailsEnabled],
				cardPresenter,
				rowsAdapter,
			)
			QueryType.Views -> ItemRowAdapter(context, GetUserViewsRequest, cardPresenter, rowsAdapter)
			QueryType.SimilarMovies -> ItemRowAdapter(context, browseRowDef.similarQuery, QueryType.SimilarMovies, cardPresenter, rowsAdapter)
			QueryType.LiveTvChannel -> ItemRowAdapter(context, browseRowDef.tvChannelQuery, LIVE_TV_CHANNEL_CHUNK_SIZE, cardPresenter, rowsAdapter)
			QueryType.LiveTvRecording -> ItemRowAdapter(context, browseRowDef.recordingQuery, browseRowDef.chunkSize, cardPresenter, rowsAdapter)
			QueryType.Resume -> ItemRowAdapter(
				context,
				browseRowDef.resumeQuery,
				browseRowDef.chunkSize,
				browseRowDef.preferParentThumb,
				browseRowDef.isStaticHeight,
				cardPresenter,
				rowsAdapter,
			)
			else -> ItemRowAdapter(
				context,
				browseRowDef.query,
				browseRowDef.chunkSize,
				browseRowDef.preferParentThumb,
				browseRowDef.isStaticHeight,
				cardPresenter,
				rowsAdapter,
				browseRowDef.queryType,
			)
		}

		rowAdapter.setReRetrieveTriggers(browseRowDef.changeTriggers)
		// Velella: each load replaces the row's cards, so the See all card goes back at the end after each one
		seeAll?.let { library ->
			@Suppress("DEPRECATION")
			rowAdapter.setRetrieveFinishedListener(object : EmptyResponse(ProcessLifecycleOwner.get().lifecycle) {
				override fun onResponse() {
					val items = List(rowAdapter.size()) { rowAdapter.get(it) }
					if (needsSeeAllCard(items)) {
						val text = context.getString(R.string.velella_see_all)
						rowAdapter.add(GridButtonBaseRowItem(SeeAllButton(library, text, items.last() as? BaseRowItem)))
					}
				}
			})
		}
		val row = ListRow(header, rowAdapter)
		rowAdapter.setRow(row)
		rowAdapter.Retrieve()
		rowsAdapter.add(row)
	}
}
