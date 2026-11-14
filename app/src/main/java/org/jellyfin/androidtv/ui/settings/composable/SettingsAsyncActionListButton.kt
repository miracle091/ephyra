package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.CircularProgressIndicator
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.design.Tokens
import timber.log.Timber

@Composable
fun <T> SettingsAsyncActionListButton(
	action: suspend CoroutineScope.() -> T,
	headingContent: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	onResult: (result: Result<T>) -> Unit = {},
	captionContent: (@Composable () -> Unit)? = null,
) {
	val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
	var state by remember { mutableStateOf(SettingsAsyncActionListButtonState.PENDING) }

	ListButton(
		modifier = modifier,
		headingContent = headingContent,
		slots = ListItemSlots(
			captionContent = captionContent,
			leadingContent = {
				when (state) {
					SettingsAsyncActionListButtonState.PENDING -> Icon(
						painter = painterResource(R.drawable.ic_upload),
						contentDescription = null
					)

					SettingsAsyncActionListButtonState.WORKING -> CircularProgressIndicator(
						modifier = Modifier.size(20.dp),
					)

					SettingsAsyncActionListButtonState.SUCCESS -> Icon(
						painter = painterResource(R.drawable.ic_check),
						tint = Tokens.Color.colorLime300,
						contentDescription = null
					)

					SettingsAsyncActionListButtonState.FAILED -> Icon(
						painter = painterResource(R.drawable.ic_error),
						tint = Tokens.Color.colorRed300,
						contentDescription = null
					)
				}
			},
		),
		onClick = {
			if (state == SettingsAsyncActionListButtonState.PENDING || state == SettingsAsyncActionListButtonState.FAILED) lifecycleScope.launch {
				state = SettingsAsyncActionListButtonState.WORKING

				runCatching {
					withContext(Dispatchers.IO) {
						action()
					}
				}.also { result ->
					state = if (result.isSuccess) SettingsAsyncActionListButtonState.SUCCESS else SettingsAsyncActionListButtonState.FAILED
					result.exceptionOrNull()?.let { error -> Timber.e(error, "Failed to execute reporting action") }
					onResult(result)
				}
			}
		}
	)
}
