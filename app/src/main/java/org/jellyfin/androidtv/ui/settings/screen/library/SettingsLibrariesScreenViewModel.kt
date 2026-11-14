package org.jellyfin.androidtv.ui.settings.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.sdk.model.api.CollectionType

private const val STOP_TIMEOUT_MILLIS = 5_000L

class SettingsLibrariesScreenViewModel(
	private val userViewsRepository: UserViewsRepository,
) : ViewModel() {
	val userViews = userViewsRepository.views
		.map { it.toList() }
		.stateIn(
			viewModelScope,
			SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
			emptyList()
		)

	fun allowGridView(collectionType: CollectionType?) = userViewsRepository.allowGridView(collectionType)
}
