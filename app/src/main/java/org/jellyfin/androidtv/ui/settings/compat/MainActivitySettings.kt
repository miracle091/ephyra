package org.jellyfin.androidtv.ui.settings.compat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jellyfin.androidtv.ui.settings.composable.SettingsTwoPaneDialog
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun MainActivitySettings() {
	val viewModel = koinActivityViewModel<SettingsViewModel>()
	val visible by viewModel.visible.collectAsState()

	SettingsTwoPaneDialog(
		visible = visible,
		onDismissRequest = { viewModel.hide() }
	)
}
