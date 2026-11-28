package org.jellyfin.androidtv.ui.playback.stillwatching

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.button.ProgressButton
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.AsyncImageSource
import org.jellyfin.androidtv.ui.composable.FixedMotionDurationScale
import org.jellyfin.androidtv.ui.composable.modifier.overscan
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

val TIMEOUT_IN_MS = 10.seconds.inWholeMilliseconds.toInt()

@Composable
private fun rememberEndWatchingTimer(item: StillWatchingItemData, onCancel: () -> Unit): Animatable<Float, AnimationVector1D> {
	val endWatchingTimer = remember { Animatable(0f) }
	LaunchedEffect(item) {
		withContext(FixedMotionDurationScale) {
			endWatchingTimer.animateTo(
				targetValue = 1f,
				animationSpec = tween(
					durationMillis = TIMEOUT_IN_MS,
					easing = LinearEasing,
				),
			)
		}
		onCancel()
	}

	return endWatchingTimer
}

@Composable
private fun StillWatchingThumbnail(thumbnail: JellyfinImage, api: ApiClient) {
	Spacer(Modifier.height(16.dp))

	AsyncImage(
		modifier = Modifier
			.height(145.dp)
			.aspectRatio(thumbnail.aspectRatio ?: 1f)
			.clip(JellyfinTheme.shapes.extraSmall),
		source = AsyncImageSource(url = thumbnail.getUrl(api), blurHash = thumbnail.blurHash),
		aspectRatio = thumbnail.aspectRatio ?: 1f,
	)
}

@Composable
private fun StillWatchingButtons(
	onCancel: () -> Unit,
	onConfirm: () -> Unit,
	endWatchingTimer: Animatable<Float, AnimationVector1D>,
	focusRequester: FocusRequester,
) {
	Row(
		modifier = Modifier
			.focusGroup()
			.focusRestorer(focusRequester)
	) {
		val coroutineScope = rememberCoroutineScope()
		ProgressButton(
			progress = endWatchingTimer.value,
			onClick = onCancel,
			modifier = Modifier
				.focusRequester(focusRequester)
				.onFocusChanged { state ->
					// Cancel timer if focus is moved away from the button
					if (!state.isFocused) {
						coroutineScope.launch {
							endWatchingTimer.snapTo(0f)
						}
					}
				},
		) {
			Text(stringResource(R.string.lbl_exit))
		}

		Spacer(Modifier.width(8.dp))

		Button(onClick = onConfirm) {
			Text(stringResource(R.string.lbl_continue_watching))
		}
	}
}

@Composable
fun StillWatchingOverlay(
	modifier: Modifier = Modifier,
	item: StillWatchingItemData,
	onConfirm: () -> Unit,
	onCancel: () -> Unit,
) = ProvideTextStyle(JellyfinTheme.typography.default.copy(color = Color.White)) {
	val api = koinInject<ApiClient>()
	val endWatchingTimer = rememberEndWatchingTimer(item, onCancel)

	val focusRequester = remember { FocusRequester() }
	Row(
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		modifier = modifier
			.overscan()
			.fillMaxWidth()
			.focusRestorer(focusRequester)
	) {
		Column(
			modifier = Modifier
				.align(Alignment.Bottom),
			horizontalAlignment = Alignment.Start
		) {
			Text(
				text = stringResource(R.string.lbl_next_up),
				style = LocalTextStyle.current.copy(fontSize = 34.sp),
			)
			Spacer(Modifier.height(4.dp))

			Text(
				text = item.title,
				style = LocalTextStyle.current.copy(fontSize = 16.sp),
				overflow = TextOverflow.Ellipsis,
				maxLines = 1,
			)

			item.thumbnail?.let { thumbnail -> StillWatchingThumbnail(thumbnail, api) }
		}

		Spacer(
			Modifier
				.weight(1f)
		)

		Column(
			modifier = Modifier
				.align(Alignment.Bottom),
			horizontalAlignment = Alignment.End
		) {
			Text(
				text = stringResource(R.string.still_watching_label),
				style = LocalTextStyle.current.copy(fontSize = 34.sp),
			)

			Spacer(Modifier.height(16.dp))

			StillWatchingButtons(onCancel, onConfirm, endWatchingTimer, focusRequester)
		}
	}
}
