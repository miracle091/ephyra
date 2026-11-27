package org.jellyfin.androidtv.ui.livetv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.lifecycle.Lifecycle
import org.jellyfin.androidtv.constant.Extras
import org.jellyfin.androidtv.ui.browsing.BrowseViewFragment
import org.jellyfin.androidtv.ui.home.homeBottomFade
import org.jellyfin.androidtv.ui.home.spotlight.SpotlightBackdrop
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar

/**
 * Velella: the Live TV screen laid out like the home: the toolbar, a spotlight of the programmes on air, and the Live TV rows
 * (BrowseViewFragment, embedded without its own title and clock) under them.
 */
class LiveTvFragment : Fragment() {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		val rowsFocusRequester = remember { FocusRequester() }
		LaunchedEffect(rowsFocusRequester) { rowsFocusRequester.requestFocus() }

		var browseFragment by remember { mutableStateOf<BrowseViewFragment?>(null) }
		val spotlight = rememberLiveTvSpotlightState(browseFragment?.view) {
			(browseFragment?.rowsFragment?.selectedPosition ?: 0) <= 0
		}
		SpotlightBackdrop(spotlight)

		Column {
			MainToolbar()

			LiveTvSpotlight(spotlight)
			AndroidFragment<BrowseViewFragment>(
				arguments = remember { bundleOf(Extras.Folder to requireArguments().getString(Extras.Folder), Extras.Embedded to true) },
				modifier = Modifier
					.focusGroup()
					.focusRequester(rowsFocusRequester)
					// As on the home: Leanback's own focus handling only lets the focus leave upwards from the first row
					.focusProperties {
						onExit = {
							val rows = browseFragment?.rowsFragment
							val isFirstRowSelected = rows?.selectedPosition?.let { it <= 0 } ?: false
							if (requestedFocusDirection != FocusDirection.Up || !isFirstRowSelected) {
								cancelFocusChange()
							} else {
								rows?.selectedPosition = 0
								rows?.verticalGridView?.clearFocus()
							}
						}
					}
					.homeBottomFade()
					.fillMaxSize(),
				maxLifecycle = Lifecycle.State.RESUMED,
				onUpdate = { fragment -> browseFragment = fragment },
			)
		}
	}
}
