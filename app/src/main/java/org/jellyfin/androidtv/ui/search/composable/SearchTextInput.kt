package org.jellyfin.androidtv.ui.search.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle

@Composable
fun SearchTextInput(
	query: String,
	onQueryChange: (query: String) -> Unit,
	onQuerySubmit: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val focusManager = LocalFocusManager.current

	val color = when {
		focused -> JellyfinTheme.colorScheme.inputFocused to JellyfinTheme.colorScheme.onInputFocused
		else -> JellyfinTheme.colorScheme.input to JellyfinTheme.colorScheme.onInput
	}

	ProvideTextStyle(
		LocalTextStyle.current.copy(
			color = color.second,
			fontSize = 16.sp,
		)
	) {
		BasicTextField(
			// Ephyra: the text field keeps up and down for its cursor, so on a remote focus could never leave it upwards (to the
			// toolbar) and the D-pad seemed dead; up and down move the focus instead
			modifier = modifier.onPreviewKeyEvent { event ->
				val direction = when (event.key) {
					Key.DirectionUp -> FocusDirection.Up
					Key.DirectionDown -> FocusDirection.Down
					else -> return@onPreviewKeyEvent false
				}
				if (event.type == KeyEventType.KeyDown) focusManager.moveFocus(direction)
				true
			},
			value = query,
			singleLine = true,
			interactionSource = interactionSource,
			onValueChange = { onQueryChange(it) },
			keyboardActions = KeyboardActions { onQuerySubmit() },
			keyboardOptions = KeyboardOptions.Default.copy(
				keyboardType = KeyboardType.Text,
				imeAction = ImeAction.Search,
				autoCorrectEnabled = true,
				// Note: Compose does not support a press to open functionality (yet?) or programmatic keyboard activation so we can only
				// use the show on focus behavior. Unfortunately this does not work great with some vendors like Amazon.
				// In addition, this boolean cannot be unset with the (stateless) BasicTextField implementation we're using
				showKeyboardOnFocus = true,
			),
			textStyle = LocalTextStyle.current,
			// Ephyra: the cursor takes the text colour; the field's own colour made it invisible on the filled field
			cursorBrush = SolidColor(color.second),
			decorationBox = { innerTextField ->
				// Ephyra: a filled field like the sign-in inputs (surface-2, solid ink when focused, the 12dp input radius), so the
				// focused field and its text are visible in every theme
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier
						.background(color.first, RoundedCornerShape(12.dp))
						.padding(horizontal = 16.dp, vertical = 12.dp)
				) {
					Icon(ImageVector.vectorResource(R.drawable.ic_search), contentDescription = null)
					Spacer(Modifier.width(12.dp))
					innerTextField()
				}
			}
		)
	}
}
