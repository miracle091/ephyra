package org.jellyfin.androidtv.ui.home.spotlight

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.ui.home.HomeRowsFragment
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

private val SpotlightInterval = 10.seconds

/**
 * Velella: the items of a spotlight (the home's titles, the Live TV programmes on air) and the one showing, shared by its backdrop
 * and its text.
 */
@Stable
class SpotlightState internal constructor() {
	var items by mutableStateOf(emptyList<BaseItemDto>())
		internal set
	var index by mutableIntStateOf(0)
		internal set
	internal var firstRowSelected by mutableStateOf(true)
	internal var focused by mutableStateOf(false)

	/** The item showing, null while the rows below the first one are selected or nothing has loaded. */
	val item: BaseItemDto? get() = if (firstRowSelected && items.isNotEmpty()) items[index % items.size] else null
}

/**
 * Velella: the spotlight at the top of the home screen (the `DetailHero` "feature" of the design in guide 5.1e): recent films and
 * series not watched yet, one at a time, with Play, Details and Favorite. It shows while the first home row is selected and
 * stops rotating while it has the focus. Null when the setting turns it off.
 */
@Composable
fun rememberHomeSpotlightState(rowsFragment: HomeRowsFragment?): SpotlightState? {
	val preferences = koinInject<VelellaPreferences>()
	if (!preferences[VelellaPreferences.homeSpotlight]) return null

	val api = koinInject<ApiClient>()
	val state = rememberSpotlightState(rowsFragment?.view) { (rowsFragment?.selectedPosition ?: 0) <= 0 }
	LaunchedEffect(api) { state.items = loadSpotlightItems(api) }
	return state
}

/**
 * Velella: a spotlight above the rows in [rowsView], showing while [isFirstRowSelected] and rotating its items every few seconds
 * while it does not have the focus. The caller loads the items.
 */
@Composable
fun rememberSpotlightState(rowsView: View?, isFirstRowSelected: () -> Boolean): SpotlightState {
	val state = remember { SpotlightState() }
	TrackFirstRowSelected(rowsView, isFirstRowSelected, state)
	// The rotation goes on where it was when the spotlight shows again
	LaunchedEffect(state.items, state.focused, state.firstRowSelected) {
		while (state.firstRowSelected && !state.focused && state.items.size > 1) {
			delay(SpotlightInterval)
			state.index = (state.index + 1) % state.items.size
		}
	}
	return state
}

/** Tracks whether the first row in [rowsView] is selected, through focus changes (Leanback has no Compose state). */
@Composable
private fun TrackFirstRowSelected(rowsView: View?, isFirstRowSelected: () -> Boolean, state: SpotlightState) {
	val firstRowSelected by rememberUpdatedState(isFirstRowSelected)
	DisposableEffect(rowsView) {
		val listener = ViewTreeObserver.OnGlobalFocusChangeListener { _, _ ->
			// The row selection settles after the focus moves
			rowsView?.post { state.firstRowSelected = firstRowSelected() }
		}
		rowsView?.viewTreeObserver?.addOnGlobalFocusChangeListener(listener)
		onDispose { rowsView?.viewTreeObserver?.removeOnGlobalFocusChangeListener(listener) }
	}
}
