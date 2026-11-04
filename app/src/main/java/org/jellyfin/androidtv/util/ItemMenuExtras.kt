package org.jellyfin.androidtv.util

import android.view.Menu
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UpdateUserItemDataDto
import org.koin.java.KoinJavaComponent
import timber.log.Timber

/** Ephyra: entries added to the MENU-key menu of a card, after upstream's own (see [KeyProcessor.createItemMenu]). */
enum class ItemMenuExtra(
	@StringRes val label: Int,
) {
	/** Clears the resume position only; the item stays watched or unwatched as it was. */
	RESET_PROGRESS(R.string.ephyra_menu_reset_progress),
	GO_TO_SERIES(R.string.lbl_goto_series),
	;

	/** Menu item id, above upstream's MENU_* constants. */
	val menuId get() = MENU_ID_BASE + ordinal

	companion object {
		private const val MENU_ID_BASE = 100

		fun forMenuId(id: Int) = entries.firstOrNull { it.menuId == id }

		fun forItem(item: BaseItemDto): List<ItemMenuExtra> = buildList {
			if ((item.userData?.playbackPositionTicks ?: 0) > 0) add(RESET_PROGRESS)
			if (item.type == BaseItemKind.EPISODE && item.seriesId != null) add(GO_TO_SERIES)
		}

		/** Adds the entries for [item] from [order] on and returns the next free order. */
		@JvmStatic
		fun addTo(menu: Menu, item: BaseItemDto, order: Int): Int {
			var next = order
			for (extra in forItem(item)) menu.add(0, extra.menuId, next++, extra.label)
			return next
		}

		/** Runs the entry with [menuId]; false when the id is not one of these entries. */
		@JvmStatic
		fun onClick(menuId: Int, item: BaseItemDto, activity: FragmentActivity): Boolean {
			when (forMenuId(menuId) ?: return false) {
				RESET_PROGRESS -> resetProgress(item, activity)
				GO_TO_SERIES -> item.seriesId?.let {
					KoinJavaComponent.get<NavigationRepository>(NavigationRepository::class.java).navigate(Destinations.itemDetails(it))
				}
			}
			return true
		}

		private fun resetProgress(item: BaseItemDto, activity: FragmentActivity) {
			val api = KoinJavaComponent.get<ApiClient>(ApiClient::class.java)
			activity.lifecycleScope.launch {
				try {
					withContext(Dispatchers.IO) {
						api.itemsApi.updateItemUserData(itemId = item.id, data = UpdateUserItemDataDto(playbackPositionTicks = 0))
					}
				} catch (err: ApiClientException) {
					Timber.w(err, "Unable to reset the progress of ${item.id}")
					return@launch
				}
				KoinJavaComponent.get<CustomMessageRepository>(CustomMessageRepository::class.java)
					.pushMessage(CustomMessage.RefreshCurrentItem)
			}
		}
	}
}
