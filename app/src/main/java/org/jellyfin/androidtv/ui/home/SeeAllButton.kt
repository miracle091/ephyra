package org.jellyfin.androidtv.ui.home

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.GridButtonBaseRowItem
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * Velella: the "See all" card at the end of a home row, opening [library] as its tile in My media does. The card takes the shape
 * of [neighbour], the row's last card, so it lines up with the posters or thumbnails before it.
 */
class SeeAllButton(
	val library: BaseItemDto,
	text: String,
	val neighbour: BaseRowItem?,
) : GridButton(ID, text, iconRes = R.drawable.ic_see_all) {
	companion object {
		const val ID = 6000
	}
}

/** Velella: whether a row holding [items] still needs its See all card: it has items and does not end with one already. */
fun needsSeeAllCard(items: List<Any?>): Boolean =
	items.isNotEmpty() && (items.last() as? GridButtonBaseRowItem)?.gridButton !is SeeAllButton
