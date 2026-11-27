package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

/** Velella: the trailer settings: the user's own Invidious instance, tried before the public ones. */
@Composable
fun SettingsPlaybackTrailersScreen() {
	val preferences = koinInject<VelellaPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.velella_pref_trailers)) },
				captionContent = { Text(stringResource(R.string.velella_pref_trailers_description)) },
			)
		}

		item {
			var instance by rememberPreference(preferences, VelellaPreferences.invidiousInstance)

			Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
				Text(
					text = stringResource(R.string.velella_pref_trailers_instance),
					style = LocalTextStyle.current.copy(color = LocalVelellaPalette.current.ink, fontSize = 16.sp),
				)
				InstanceField(
					value = instance,
					onValueChange = { instance = it.trim() },
					modifier = Modifier
						.padding(vertical = 8.dp)
						.fillMaxWidth()
						.focusKey("invidious_instance"),
				)
				Text(
					text = stringResource(R.string.velella_pref_trailers_instance_description),
					style = LocalTextStyle.current.copy(fontSize = 13.sp, color = LocalVelellaPalette.current.inkMuted),
				)
			}
		}
	}
}

/** A filled one-line field for an address, like the search field; up and down move the focus instead of the cursor. */
@Composable
private fun InstanceField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
	val interactionSource = remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val focusManager = LocalFocusManager.current
	val (container, content) = when {
		focused -> JellyfinTheme.colorScheme.inputFocused to JellyfinTheme.colorScheme.onInputFocused
		else -> JellyfinTheme.colorScheme.input to JellyfinTheme.colorScheme.onInput
	}

	BasicTextField(
		value = value,
		onValueChange = onValueChange,
		singleLine = true,
		interactionSource = interactionSource,
		keyboardOptions = KeyboardOptions.Default.copy(
			keyboardType = KeyboardType.Uri,
			imeAction = ImeAction.Done,
			autoCorrectEnabled = false,
		),
		textStyle = LocalTextStyle.current.copy(color = content, fontSize = 16.sp),
		cursorBrush = SolidColor(content),
		modifier = modifier.onPreviewKeyEvent { event ->
			val direction = when (event.key) {
				Key.DirectionUp -> FocusDirection.Up
				Key.DirectionDown -> FocusDirection.Down
				else -> return@onPreviewKeyEvent false
			}
			if (event.type == KeyEventType.KeyDown) focusManager.moveFocus(direction)
			true
		},
		decorationBox = { innerTextField ->
			Box(
				modifier = Modifier
					.background(container, RoundedCornerShape(12.dp))
					.padding(horizontal = 16.dp, vertical = 12.dp)
			) {
				if (value.isEmpty()) Text("https://invidious.example", style = LocalTextStyle.current.copy(color = content.copy(alpha = 0.5f)))
				innerTextField()
			}
		},
	)
}
