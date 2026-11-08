package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette

/** Width of the left pane (title and sections) of the two-pane settings. */
internal val SettingsLeftPaneWidth = 300.dp

/** Space around the content of the right pane. SettingsColumn already pads its content with the medium space token. */
internal val SettingsRightPanePadding = PaddingValues(start = 8.dp, top = 11.dp, end = 16.dp, bottom = 11.dp)

/**
 * The panel of a settings screen that opens on its own (not as a section of the two-pane settings): it takes the place of the right pane
 * of [SettingsTwoPaneDialog], so it has the same width, surface and spacing. The scrim stays visible where the left pane would be.
 */
@Composable
fun SettingsLayout(
	modifier: Modifier = Modifier,
	content: @Composable BoxScope.() -> Unit,
) {
	Box(
		modifier = modifier
			.fillMaxSize()
			.padding(start = SettingsLeftPaneWidth)
			.background(LocalEphyraPalette.current.surface1)
			.clipToBounds()
			.padding(SettingsRightPanePadding)
			.focusRestorer(),
		content = content
	)
}
