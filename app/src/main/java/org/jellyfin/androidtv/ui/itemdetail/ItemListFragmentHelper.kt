package org.jellyfin.androidtv.ui.itemdetail

import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.TextUnderButton
import org.jellyfin.androidtv.util.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.playlistsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.koin.android.ext.android.inject
import java.util.UUID

fun ItemListFragment.loadItem(itemId: UUID) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val item = withContext(Dispatchers.IO) {
			api.userLibraryApi.getItem(itemId).content
		}
		if (isActive) setBaseItem(item)
	}
}

fun MusicFavoritesListFragment.getFavoritePlaylist(
	parentId: UUID?,
	callback: (items: List<BaseItemDto>) -> Unit
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val result = withContext(Dispatchers.IO) {
			api.itemsApi.getItems(
				parentId = parentId,
				includeItemTypes = setOf(BaseItemKind.AUDIO),
				recursive = true,
				filters = setOf(org.jellyfin.sdk.model.api.ItemFilter.IS_FAVORITE_OR_LIKES),
				sortBy = setOf(ItemSortBy.RANDOM),
				limit = 100,
				fields = ItemRepository.itemFields,
			).content
		}

		callback(result.items)
	}
}

fun ItemListFragment.getPlaylist(
	item: BaseItemDto,
	callback: (items: List<BaseItemDto>) -> Unit
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val result = withContext(Dispatchers.IO) {
			when {
				item.type == BaseItemKind.PLAYLIST -> api.playlistsApi.getPlaylistItems(
					playlistId = item.id,
					limit = 150,
					fields = ItemRepository.itemFields,
				).content

				else -> api.itemsApi.getItems(
					parentId = item.id,
					includeItemTypes = setOf(BaseItemKind.AUDIO),
					recursive = true,
					sortBy = setOf(ItemSortBy.SORT_NAME),
					limit = 200,
					fields = ItemRepository.itemFields,
				).content
			}
		}

		callback(result.items)
	}
}

fun ItemListFragment.toggleFavorite(item: BaseItemDto, callback: (item: BaseItemDto) -> Unit) {
	val itemMutationRepository by inject<ItemMutationRepository>()

	lifecycleScope.launch {
		val userData = itemMutationRepository.setFavorite(
			item = item.id,
			favorite = !(item.userData?.isFavorite ?: false)
		)
		callback(item.copy(userData = userData))
	}
}

// The home spotlight's buttons: 40dp, 8dp apart, 20dp icons
private const val ICON_BUTTON_GAP_DP = 8
private const val ICON_BUTTON_SIZE_DP = 40
private const val ICON_BUTTON_PADDING_DP = 10

/**
 * Ephyra: the album and details buttons are icon buttons like the home spotlight's: no fill until focused, the icon in ink, in
 * the accent when active; the label stays as the content description for TalkBack.
 */
fun showButtonIconsOnly(buttonRow: ViewGroup) {
	buttonRow.children.filterIsInstance<TextUnderButton>().forEach { button ->
		val context = button.context
		button.labelVisible = false
		button.updateLayoutParams<ViewGroup.MarginLayoutParams> { marginEnd = ICON_BUTTON_GAP_DP.dp(context) }
		button.binding.imageButton.apply {
			updateLayoutParams {
				width = ICON_BUTTON_SIZE_DP.dp(context)
				height = ICON_BUTTON_SIZE_DP.dp(context)
			}
			maxHeight = Int.MAX_VALUE
			setPadding(ICON_BUTTON_PADDING_DP.dp(context))
			setBackgroundResource(R.drawable.button_icon_back)
			ImageViewCompat.setImageTintList(this, ContextCompat.getColorStateList(context, R.color.ephyra_icon_button_tint))
			// The play button already has the focus before it is attached: without this its fill's fade-in never ends
			jumpDrawablesToCurrentState()
		}
	}
}
