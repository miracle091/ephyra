package org.jellyfin.androidtv.ui.presentation

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.RowPresenter
import org.jellyfin.androidtv.ui.composable.item.ROW_ITEM_GAP_DP

open class CustomListRowPresenter @JvmOverloads constructor(
	private val topPadding: Int? = null
) : ListRowPresenter() {
	init {
		headerPresenter = CustomRowHeaderPresenter()
	}

	override fun isUsingDefaultShadow() = false

	// Ephyra: a gap the focused card's zoom does not cover, so its ring stays off the next card
	override fun createRowViewHolder(parent: ViewGroup): RowPresenter.ViewHolder = super.createRowViewHolder(parent).also { holder ->
		val gap = (ROW_ITEM_GAP_DP * parent.resources.displayMetrics.density).toInt()
		(holder as? ViewHolder)?.gridView?.horizontalSpacing = gap
	}

	override fun onSelectLevelChanged(holder: RowPresenter.ViewHolder) = Unit

	override fun onBindRowViewHolder(holder: RowPresenter.ViewHolder, item: Any) {
		super.onBindRowViewHolder(holder, item)

		val view = holder.view?.parent as? View ?: return
		if (topPadding != null) view.setPadding(view.paddingLeft, topPadding, view.paddingRight, view.paddingBottom)

		// Hide header view when the item doesn't have one
		holder.headerViewHolder.view.isVisible = !(item is ListRow && item.headerItem == null)
	}
}
