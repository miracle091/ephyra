package org.jellyfin.androidtv.ui.itemdetail

import androidx.core.view.isVisible
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.data.model.InfoItem
import org.jellyfin.androidtv.ui.TextUnderButton
import org.jellyfin.sdk.model.api.BaseItemDto

class MyDetailsOverviewRow @JvmOverloads constructor(
	val item: BaseItemDto,
	var imageDrawable: String? = null,
	var summary: String? = null,
	var infoItem1: InfoItem? = null,
	var infoItem2: InfoItem? = null,
	var infoItem3: InfoItem? = null,
) : Row() {
	var selectedMediaSourceIndex: Int = 0

	/** Velella: the series' next up episode shown as a card beside the buttons, and what a click on it does. */
	var nextUpEpisode: BaseItemDto? = null
	var onNextUpClick: ((BaseItemDto) -> Unit)? = null

	private val _actions = mutableListOf<TextUnderButton>()
	val actions get() = _actions.toList()
	val visibleActions get() = _actions.count { it.isVisible }

	fun clearActions() = _actions.clear()
	fun addAction(button: TextUnderButton) = _actions.add(button)
}
