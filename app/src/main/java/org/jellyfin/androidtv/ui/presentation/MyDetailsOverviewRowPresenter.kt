package org.jellyfin.androidtv.ui.presentation

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.leanback.widget.RowPresenter
import org.jellyfin.androidtv.ui.DetailRowView
import org.jellyfin.androidtv.ui.browsing.composable.inforow.BaseItemInfoRowView
import org.jellyfin.androidtv.ui.itemdetail.DetailsTagline
import org.jellyfin.androidtv.ui.itemdetail.MyDetailsOverviewRow
import org.jellyfin.androidtv.ui.itemdetail.showButtonIconsOnly
import org.jellyfin.androidtv.util.InfoLayoutHelper
import org.jellyfin.androidtv.util.MarkdownRenderer
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

class MyDetailsOverviewRowPresenter(
	private val markdownRenderer: MarkdownRenderer,
) : RowPresenter() {
	class ViewHolder(
		private val detailRowView: DetailRowView,
		private val markdownRenderer: MarkdownRenderer,
	) : RowPresenter.ViewHolder(detailRowView) {
		private val binding get() = detailRowView.binding

		fun setItem(row: MyDetailsOverviewRow) {
			setTitle(row.item.name)

			InfoLayoutHelper.addInfoRow(view.context, row.item, row.item.mediaSources?.getOrNull(row.selectedMediaSourceIndex), binding.fdMainInfoRow, false)
			// Ephyra: groups divided by vertical lines and the official rating as an outlined badge
			binding.fdMainInfoRow.children.filterIsInstance<BaseItemInfoRowView>().forEach { it.separated = true }
			binding.fdGenreRow.text = row.item.genres?.joinToString(" · ") // Ephyra: the spotlight's separator

			// Ephyra: the tagline under the title, hidden when the item has none
			val tagline = DetailsTagline.of(row.item.taglines)
			binding.fdTagline.text = tagline
			binding.fdTagline.isVisible = tagline != null

			binding.infoTitle1.text = row.infoItem1?.label
			binding.infoValue1.text = row.infoItem1?.value

			binding.infoTitle2.text = row.infoItem2?.label
			binding.infoValue2.text = row.infoItem2?.value

			binding.infoTitle3.text = row.infoItem3?.label
			binding.infoValue3.text = row.infoItem3?.value

			binding.mainImage.load(row.imageDrawable, null, null, 1.0, 0)

			setSummary(row.summary)

			// Ephyra: a person has no genres, but the empty genre line keeps its space so the name sits where the other details
			// put the title, on the clock's line
			binding.fdGenreRow.isInvisible = row.item.type == BaseItemKind.PERSON

			setNextUp(row.nextUpEpisode, row.onNextUpClick)

			binding.fdButtonRow.removeAllViews()
			for (button in row.actions) {
				val parent = button.parent
				if (parent is ViewGroup) parent.removeView(button)

				binding.fdButtonRow.addView(button)
			}
			// Ephyra: icon buttons like the album's, and no empty label/value pairs
			showButtonIconsOnly(binding.fdButtonRow)
			listOf(binding.infoTitle1, binding.infoTitle2, binding.infoTitle3).forEach { title ->
				(title.parent as? View)?.isVisible = !title.text.isNullOrBlank()
			}
		}

		/** Ephyra: shows the series' next up episode as a card beside the buttons, or hides the card when there is none. */
		fun setNextUp(episode: BaseItemDto?, onClick: ((BaseItemDto) -> Unit)?) {
			binding.fdNextUpCard.episode = episode
			binding.fdNextUpCard.setOnClickListener {
				if (episode != null) onClick?.invoke(episode)
			}
		}

		fun setTitle(title: String?) {
			binding.fdTitle.text = title
		}

		fun setSummary(summary: String?) {
			binding.fdSummaryText.text = summary?.let { markdownRenderer.toMarkdownSpanned(it) }
			// Ephyra: no empty gap when there is no overview (a person without a biography): the buttons move up under the info row
			binding.fdSummaryText.isVisible = !summary.isNullOrBlank()
		}

		fun setInfoValue3(text: String?) {
			binding.infoValue3.text = text
		}
	}

	var viewHolder: ViewHolder? = null
		private set

	init {
		syncActivatePolicy = SYNC_ACTIVATED_CUSTOM
	}

	override fun createRowViewHolder(parent: ViewGroup): ViewHolder {
		val view = DetailRowView(parent.context)
		viewHolder = ViewHolder(view, markdownRenderer)
		return viewHolder!!
	}

	override fun onBindRowViewHolder(viewHolder: RowPresenter.ViewHolder, item: Any) {
		super.onBindRowViewHolder(viewHolder, item)
		if (item !is MyDetailsOverviewRow) return
		if (viewHolder !is ViewHolder) return

		viewHolder.setItem(item)
	}

	override fun onSelectLevelChanged(holder: RowPresenter.ViewHolder) = Unit
}
