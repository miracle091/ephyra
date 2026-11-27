package org.jellyfin.androidtv.ui.player.video

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import org.jellyfin.androidtv.preference.LiveTvPreferences
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.livetv.getLiveTvChannelList
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject
import timber.log.Timber
import java.util.UUID

private val CARD_WIDTH = 220.dp
private val LOGO_HEIGHT = 96.dp
private const val SCRIM_ALPHA = 0.85f
private const val LOGO_MAX_WIDTH = 440

/** Velella: whether the live player's channel bar is open, and how many times it closed (the overlay then takes the focus back). */
@Stable
class ChannelBarState {
	var isOpen by mutableStateOf(false)
		private set
	var closings by mutableIntStateOf(0)
		private set

	fun open() {
		isOpen = true
	}

	fun close() {
		isOpen = false
		closings++
	}
}

/** Velella: the channel bar of a [live] channel at the bottom of the player while it is open; tuning to a channel closes it. */
@Composable
fun BoxScope.LiveChannelBarHost(state: ChannelBarState, live: LiveControls?) {
	if (!state.isOpen || live == null) return
	LiveChannelBar(
		currentChannel = live.info.channel.id,
		onSwitch = { channelId ->
			state.close()
			live.actions.onSwitch(channelId)
		},
		onDismiss = state::close,
		modifier = Modifier.align(Alignment.BottomCenter),
	)
}

/**
 * Velella: the live player's channel bar after the legacy player's quick channel changer (showQuickChannelChanger): a row of the
 * channels in the guide's order at the bottom of the picture, the current one focused. OK tunes to the focused channel, Back
 * closes the bar.
 */
@Composable
fun LiveChannelBar(
	currentChannel: UUID,
	onSwitch: (UUID) -> Unit,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val api = koinInject<ApiClient>()
	val liveTvPreferences = koinInject<LiveTvPreferences>()
	BackHandler(onBack = onDismiss)
	val channels by produceState<List<BaseItemDto>?>(null) {
		value = try {
			api.getLiveTvChannelList(liveTvPreferences)
		} catch (error: CancellationException) {
			throw error
		} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
			Timber.w(error, "Unable to get the channels")
			emptyList()
		}
	}

	val palette = LocalVelellaPalette.current
	Box(
		contentAlignment = Alignment.BottomStart,
		modifier = modifier
			.fillMaxWidth()
			.background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = SCRIM_ALPHA))))
			.padding(top = 48.dp, bottom = 32.dp),
	) {
		val list = channels ?: return@Box
		if (list.isEmpty()) {
			LaunchedEffect(Unit) { onDismiss() }
			return@Box
		}

		val startIndex = remember(list) { list.indexOfFirst { it.id == currentChannel }.coerceAtLeast(0) }
		val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
		val focusRequester = remember { FocusRequester() }
		LaunchedEffect(list) { focusRequester.requestFocus() }

		LazyRow(
			state = listState,
			contentPadding = PaddingValues(horizontal = 32.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
		) {
			itemsIndexed(list, key = { _, channel -> channel.id }) { index, channel ->
				ChannelCard(
					channel = channel,
					current = channel.id == currentChannel,
					api = api,
					onClick = { onSwitch(channel.id) },
					modifier = if (index == startIndex) Modifier.focusRequester(focusRequester) else Modifier,
				)
			}
		}
	}
}

/** A channel: its logo on surface-1 (an accent ring when focused), the number and name, then the programme on air. */
@Composable
private fun ChannelCard(
	channel: BaseItemDto,
	current: Boolean,
	api: ApiClient,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val palette = LocalVelellaPalette.current
	val interactionSource = remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val shape = RoundedCornerShape(8.dp)

	Column(
		modifier = modifier
			.width(CARD_WIDTH)
			.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
			.focusable(interactionSource = interactionSource),
	) {
		Box(
			contentAlignment = Alignment.Center,
			modifier = Modifier
				.fillMaxWidth()
				.height(LOGO_HEIGHT)
				.clip(shape)
				.background(if (focused) palette.surface3 else palette.surface1)
				.border(2.dp, if (focused) palette.accent else Color.Transparent, shape),
		) {
			val logo = remember(channel) { channel.itemImages[ImageType.PRIMARY]?.getUrl(api, maxWidth = LOGO_MAX_WIDTH) }
			if (logo != null) {
				AsyncImage(
					model = logo,
					contentDescription = null,
					contentScale = ContentScale.Fit,
					modifier = Modifier.fillMaxSize().padding(16.dp),
				)
			}
		}

		val style = LocalTextStyle.current
		Text(
			text = listOfNotNull(channel.number, channel.name).filter { it.isNotBlank() }.joinToString(" · "),
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			style = style.merge(
				color = if (current) palette.accent else palette.ink,
				fontSize = 14.sp,
				fontWeight = FontWeight.Bold,
			),
			modifier = Modifier.padding(top = 8.dp),
		)
		Text(
			text = channel.currentProgram?.name.orEmpty(),
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			style = style.merge(color = palette.inkMuted, fontSize = 12.sp),
			modifier = Modifier.padding(top = 2.dp),
		)
	}
}
