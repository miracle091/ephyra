package org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.composable

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.util.ListColorChannelRangeControl

/** The "custom colour" section of the subtitle colour screens: a heading and one range control per colour channel. */
fun LazyListScope.subtitleColorChannelItems(
	value: Color,
	onValueChange: (color: Color) -> Unit,
	includeAlpha: Boolean = true,
) {
	item { ListSection(headingContent = { Text(stringResource(R.string.color_custom)) }) }

	item { ColorChannelItem(R.string.color_red, Color.Red, "custom_red", value, onValueChange) }
	item { ColorChannelItem(R.string.color_green, Color.Green, "custom_green", value, onValueChange) }
	item { ColorChannelItem(R.string.color_blue, Color.Blue, "custom_blue", value, onValueChange) }
	if (includeAlpha) item { ColorChannelItem(R.string.color_alpha, Color.Transparent, "custom_alpha", value, onValueChange) }
}

@Composable
private fun ColorChannelItem(
	@StringRes heading: Int,
	channel: Color,
	key: String,
	value: Color,
	onValueChange: (color: Color) -> Unit,
) {
	ListColorChannelRangeControl(
		headingContent = { Text(stringResource(heading)) },
		channel = channel,
		value = value,
		onValueChange = onValueChange,
		modifier = Modifier.focusKey(key)
	)
}
