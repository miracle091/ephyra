@file:JvmName("FullDetailsFragmentHelperKt")
@file:JvmMultifileClass

package org.jellyfin.androidtv.ui.itemdetail

import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.apiclient.getSeriesOverview
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import org.jellyfin.sdk.model.serializer.toUUID
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.UUID

fun FullDetailsFragment.createFakeSeriesTimerBaseItemDto(timer: SeriesTimerInfoDto) = BaseItemDto(
	id = requireNotNull(timer.id).toUUID(),
	type = BaseItemKind.FOLDER,
	mediaType = MediaType.UNKNOWN,
	seriesTimerId = timer.id,
	name = timer.name,
	overview = timer.getSeriesOverview(requireContext()),
)

fun FullDetailsFragment.getLiveTvSeriesTimer(
	id: String,
	callback: (timer: SeriesTimerInfoDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getSeriesTimer(id).content
			}
		}.onSuccess { timer ->
			callback(timer)
		}
	}
}

fun FullDetailsFragment.getLiveTvProgram(
	id: UUID,
	callback: (program: BaseItemDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getProgram(id.toString()).content
			}
		}.onSuccess { program ->
			callback(program)
		}
	}
}

fun FullDetailsFragment.createLiveTvSeriesTimer(
	seriesTimer: SeriesTimerInfoDto,
	callback: () -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.createSeriesTimer(seriesTimer)
			}
		}.onSuccess {
			callback()
		}
	}
}

fun FullDetailsFragment.getLiveTvDefaultTimer(
	id: UUID,
	callback: (seriesTimer: SeriesTimerInfoDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getDefaultTimer(id.toString()).content
			}
		}.onSuccess { seriesTimer ->
			callback(seriesTimer)
		}
	}
}

fun FullDetailsFragment.cancelLiveTvSeriesTimer(
	timerId: String,
	callback: () -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.cancelTimer(timerId)
			}
		}.onSuccess {
			callback()
		}
	}
}

fun FullDetailsFragment.getLiveTvChannel(
	id: UUID,
	callback: (channel: BaseItemDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getChannel(id).content
			}
		}.onSuccess { channel ->
			callback(channel)
		}
	}
}

/** Ephyra: shows the delete button only for a live TV recording the server lets this user delete (see [canDeleteFromTv]). */
fun FullDetailsFragment.showDeleteButtonForRecording() {
	val item = mBaseItem
	if (item.canDelete != true) return

	val api by inject<ApiClient>()
	val userRepository by inject<UserRepository>()

	lifecycleScope.launch {
		val allowed = try {
			withContext(Dispatchers.IO) {
				val userId = userRepository.currentUser.value?.id
				val recordingFolderIds = api.liveTvApi.getRecordingFolders(userId).content.items.map { it.id }
				val ancestorIds = api.libraryApi.getAncestors(item.id, userId).content.map { it.id }
				canDeleteFromTv(item, ancestorIds, recordingFolderIds)
			}
		} catch (error: ApiClientException) {
			Timber.w(error, "Unable to tell whether ${item.id} is a recording")
			false
		}

		deleteButton?.isVisible = allowed
		showMoreButtonIfNeeded()
	}
}

fun FullDetailsFragment.deleteRecording() {
	val item = mBaseItem
	val api by inject<ApiClient>()
	val dataRefreshService by inject<DataRefreshService>()
	val navigationRepository by inject<NavigationRepository>()

	lifecycleScope.launch {
		Timber.i("Deleting recording ${item.id}")
		try {
			withContext(Dispatchers.IO) { api.libraryApi.deleteItem(item.id) }
		} catch (error: ApiClientException) {
			Timber.e(error, "Failed to delete recording ${item.id}")
			Toast.makeText(context, getString(R.string.item_deletion_failed, item.name), Toast.LENGTH_LONG).show()
			return@launch
		}

		dataRefreshService.lastDeletedItemId = item.id
		if (navigationRepository.canGoBack) navigationRepository.goBack() else navigationRepository.navigate(Destinations.home)
		Toast.makeText(context, getString(R.string.item_deleted, item.name), Toast.LENGTH_LONG).show()
	}
}
