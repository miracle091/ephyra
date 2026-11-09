package org.jellyfin.androidtv.ui.browsing

import androidx.core.view.doOnPreDraw
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.PresenterSelector
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.querying.GetSpecialsRequest
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.preference.EphyraPreferences
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.CompactEpisodePresenter
import org.jellyfin.androidtv.ui.presentation.CompactEpisodeRow
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

class GenericFolderFragment : EnhancedBrowseFragment() {
	private val ephyraPreferences by inject<EphyraPreferences>()
	private val api by inject<ApiClient>()

	// Ephyra: the row of the season's episodes in the standard layout, which shows their meta line
	private var episodesRowDef: BrowseRowDef? = null

	private val showsCompactEpisodes get() = ephyraPreferences[EphyraPreferences.compactEpisodeList]

	companion object {
		private const val ROW_PAGE_SIZE = 100
		private const val EPISODE_CARD_HEIGHT = 140

		private val showSpecialViewTypes = setOf(
			BaseItemKind.COLLECTION_FOLDER,
			BaseItemKind.FOLDER,
			BaseItemKind.USER_VIEW,
			BaseItemKind.CHANNEL_FOLDER_ITEM,
		)
	}

	override fun setupQueries(rowLoader: RowLoader) {
		if ((mFolder.childCount == null || mFolder.childCount == 0) && !setOf(
				BaseItemKind.CHANNEL,
				BaseItemKind.CHANNEL_FOLDER_ITEM,
				BaseItemKind.USER_VIEW,
				BaseItemKind.COLLECTION_FOLDER,
			).contains(mFolder.type)
		) return

		// Ephyra: split in two helpers to keep this method short
		if (showSpecialViewTypes.contains(mFolder.type)) addSpecialViewRows()

		val byName = GetItemsRequest(
			fields = ItemRepository.itemFields,
			parentId = mFolder.id,
		)
		val header = when (mFolder.type) {
			BaseItemKind.SEASON -> mFolder.name
			else -> getString(R.string.lbl_by_name)
		}

		val compactEpisodes = mFolder.type == BaseItemKind.SEASON && showsCompactEpisodes
		// Ephyra: the compact list loads the episodes itself, one row each
		if (!compactEpisodes) mRows.add(BrowseRowDef(header, byName, ROW_PAGE_SIZE).also { episodesRowDef = it })

		if (mFolder.type == BaseItemKind.SEASON) addSeasonRows()

		rowLoader.loadRows(mRows)
		if (compactEpisodes) loadCompactEpisodes(byName)
	}

	private fun addSpecialViewRows() {
		if (mFolder.type != BaseItemKind.CHANNEL_FOLDER_ITEM) {
			val resume = GetItemsRequest(
				fields = ItemRepository.itemFields,
				parentId = mFolder.id,
				limit = 50,
				filters = setOf(ItemFilter.IS_RESUMABLE),
				sortBy = setOf(ItemSortBy.DATE_PLAYED),
				sortOrder = setOf(SortOrder.DESCENDING),
			)
			mRows.add(BrowseRowDef(getString(R.string.lbl_continue_watching), resume, 0))
		}

		val latest = GetItemsRequest(
			fields = ItemRepository.itemFields,
			parentId = mFolder.id,
			limit = 50,
			filters = setOf(ItemFilter.IS_UNPLAYED),
			sortBy = setOf(ItemSortBy.DATE_CREATED),
			sortOrder = setOf(SortOrder.DESCENDING),
		)
		mRows.add(BrowseRowDef(getString(R.string.lbl_latest), latest, 0))
	}

	private fun addSeasonRows() {
		val specials = GetSpecialsRequest(mFolder.id)
		mRows.add(BrowseRowDef(getString(R.string.lbl_specials), specials))

		val seriesId = mFolder.seriesId ?: mFolder.parentId
		if (seriesId != null) {
			val otherSeasons = GetItemsRequest(
				fields = ItemRepository.itemFields,
				parentId = seriesId,
				includeItemTypes = setOf(BaseItemKind.SEASON),
				excludeItemIds = setOf(mFolder.id),
			)
			mRows.add(BrowseRowDef(getString(R.string.lbl_seasons), otherSeasons, ROW_PAGE_SIZE))

			val series = GetItemsRequest(
				fields = ItemRepository.itemFields,
				ids = setOf(seriesId),
			)
			mRows.add(BrowseRowDef(getString(R.string.lbl_series), series, ROW_PAGE_SIZE))
		}
	}

	// Ephyra: the compact list needs rows that can hold one episode each, to move between episodes with up and down
	override fun createRowPresenterSelector(): PresenterSelector {
		if (!showsCompactEpisodes) return super.createRowPresenterSelector()

		return ClassPresenterSelector()
			.addClassPresenter(CompactEpisodeRow::class.java, PositionableListRowPresenter(null, FocusHighlight.ZOOM_FACTOR_NONE))
			.addClassPresenter(ListRow::class.java, PositionableListRowPresenter())
	}

	// Ephyra: the cards of the episodes show their runtime, rating and end time under the title
	override fun getPresenterSelector(def: BrowseRowDef, defaultSelector: PresenterSelector): PresenterSelector {
		if (def !== episodesRowDef) return defaultSelector

		val cardPresenter = CardPresenter(false, EPISODE_CARD_HEIGHT).also { it.episodeTiming = true }
		return ClassPresenterSelector().addClassPresenter(BaseRowItem::class.java, cardPresenter)
	}

	private fun loadCompactEpisodes(query: GetItemsRequest) {
		lifecycleScope.launch {
			val episodes = try {
				withContext(Dispatchers.IO) { api.libraryApi.getItems(query).content.items }
			} catch (error: CancellationException) {
				throw error
			} catch (error: ApiClientException) {
				Timber.w(error, "Failed to load the episodes of the season")
				return@launch
			}
			if (!isAdded) return@launch

			val cardPresenter = CompactEpisodePresenter()
			episodes.forEachIndexed { index, episode ->
				val adapter = ItemRowAdapter(requireContext(), listOf(episode), cardPresenter, mRowsAdapter, true)
				val header = if (index == 0) HeaderItem(mFolder.name.orEmpty()) else null
				val row = CompactEpisodeRow(header, adapter)
				adapter.setRow(row)
				mRowsAdapter.add(index, row)
				adapter.Retrieve()
			}

			// The other rows were added first and the list moves the focus past the rows inserted before it when it lays them
			// out, so the first episode is selected after that layout
			val grid = mRowsFragment?.verticalGridView
			if (episodes.isNotEmpty() && grid != null) grid.doOnPreDraw { mRowsFragment?.setSelectedPosition(0, false) }
		}
	}
}
