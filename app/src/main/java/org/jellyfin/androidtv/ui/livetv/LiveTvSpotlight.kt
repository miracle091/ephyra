package org.jellyfin.androidtv.ui.livetv

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.composable.InfoRowGroups
import org.jellyfin.androidtv.ui.home.spotlight.SPOTLIGHT_SIZE
import org.jellyfin.androidtv.ui.home.spotlight.SpotlightFrame
import org.jellyfin.androidtv.ui.home.spotlight.SpotlightMetaText
import org.jellyfin.androidtv.ui.home.spotlight.SpotlightState
import org.jellyfin.androidtv.ui.home.spotlight.SpotlightTextsAboveButtons
import org.jellyfin.androidtv.ui.home.spotlight.rememberSpotlightState
import org.jellyfin.androidtv.ui.home.spotlight.spotlightOverview
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.PlaybackHelper
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.koin.compose.koinInject
import timber.log.Timber
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// More than the spotlight shows, so the ones without an image can be left out
private const val ON_AIR_CANDIDATES = 20
private val MinReload = 1.minutes
private val EndMargin = 5.seconds
private const val TITLE_MARQUEE_DELAY_MS = 1500

/**
 * Velella: the spotlight of the Live TV screen: programmes on air, one at a time, with their channel, an "on air" tag and the
 * time left, Watch and Details. It shows while the first row is selected and loads again when one of its programmes ends.
 */
@Composable
fun rememberLiveTvSpotlightState(rowsView: View?, isFirstRowSelected: () -> Boolean): SpotlightState {
	val api = koinInject<ApiClient>()
	val state = rememberSpotlightState(rowsView, isFirstRowSelected)
	LaunchedEffect(api) {
		while (true) {
			state.items = loadOnAirPrograms(api)
			delay(onAirReloadDelay(state.items, LocalDateTime.now()))
		}
	}
	return state
}

/**
 * Velella: how long until the first of [programs] ends, so the spotlight drops it then, plus a few seconds so the server no
 * longer counts it as airing; at least a minute.
 */
fun onAirReloadDelay(programs: List<BaseItemDto>, now: LocalDateTime): Duration {
	val firstEnd = programs.mapNotNull { it.endDate }.minOrNull() ?: return MinReload
	return maxOf(MinReload, java.time.Duration.between(now, firstEnd).toMillis().milliseconds + EndMargin)
}

/** Velella: up to [SPOTLIGHT_SIZE] programmes on air that have an image to show. */
suspend fun loadOnAirPrograms(api: ApiClient): List<BaseItemDto> = withContext(Dispatchers.IO) {
	try {
		api.liveTvApi.getRecommendedPrograms(
			GetRecommendedProgramsRequest(
				isAiring = true,
				fields = ItemRepository.itemFields,
				imageTypeLimit = 1,
				enableTotalRecordCount = false,
				limit = ON_AIR_CANDIDATES,
			)
		).content.items.filter { !it.imageTags.isNullOrEmpty() || !it.backdropImageTags.isNullOrEmpty() }.take(SPOTLIGHT_SIZE)
	} catch (error: CancellationException) {
		throw error
	} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
		Timber.w(error, "Could not load the programmes on air for the Live TV spotlight")
		emptyList()
	}
}

/** Velella: the Live TV spotlight's text and buttons, under the toolbar, above the rows. */
@Composable
fun LiveTvSpotlight(state: SpotlightState?, modifier: Modifier = Modifier) {
	SpotlightFrame(state, modifier) { item, dashes, infoModifier -> LiveTvSpotlightInfo(item, dashes, infoModifier) }
}

@Composable
private fun LiveTvSpotlightInfo(item: BaseItemDto, dashes: @Composable () -> Unit, modifier: Modifier = Modifier) {
	val watchFocusRequester = remember { FocusRequester() }
	Column(
		// Coming from the rows or the toolbar, the focus lands on Watch
		modifier = modifier
			.width(560.dp)
			.focusProperties { onEnter = { watchFocusRequester.requestFocus() } }
			.focusGroup(),
	) {
		SpotlightTextsAboveButtons { LiveTvSpotlightTexts(item) }
		Row(verticalAlignment = Alignment.CenterVertically) {
			LiveTvSpotlightButtons(item, watchFocusRequester)
			Spacer(Modifier.width(24.dp))
			dashes()
		}
	}
}

/** The programme's name, the info row and the overview. */
@Composable
private fun LiveTvSpotlightTexts(item: BaseItemDto) {
	Text(
		text = item.name.orEmpty(),
		style = LocalTextStyle.current.merge(
			color = LocalVelellaPalette.current.ink,
			fontSize = 28.sp,
			fontWeight = FontWeight.Bold,
			lineHeight = 34.sp,
		),
		maxLines = 1,
		// A name too long for the spotlight scrolls instead of being cut
		modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = TITLE_MARQUEE_DELAY_MS),
	)
	LiveTvSpotlightInfoRow(item)
	spotlightOverview(item)?.let { overview ->
		Text(
			text = overview,
			style = LocalTextStyle.current.merge(color = LocalVelellaPalette.current.inkMuted, fontSize = 14.sp, lineHeight = 19.sp),
			maxLines = 3,
			overflow = TextOverflow.Ellipsis,
		)
	}
}

/** The "on air" tag and the time left (moving on as each minute starts), then the channel and the episode. */
@Composable
private fun LiveTvSpotlightInfoRow(item: BaseItemDto) {
	val context = LocalContext.current
	val now by produceState(LocalDateTime.now()) {
		while (true) {
			delay(GuideLayoutHelper.delayToNextMinute(System.currentTimeMillis()))
			value = LocalDateTime.now()
		}
	}
	val end = item.endDate
	InfoRowGroups(
		buildList<@Composable () -> Unit> {
			if (end != null && now.isBefore(end)) {
				add {
					Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
						OnAirBadge()
						SpotlightMetaText(GuideStyle.timeLeftText(context, end, now))
					}
				}
			}
			item.channelName?.takeIf { it.isNotBlank() }?.let { add { SpotlightMetaText(it) } }
			item.episodeTitle?.takeIf { it.isNotBlank() && it != item.name }?.let { add { SpotlightMetaText(it) } }
		}
	)
}

/** The guide's "on air" tag: filled in the theme's accent. */
@Composable
private fun OnAirBadge() {
	val palette = LocalVelellaPalette.current
	Text(
		text = stringResource(R.string.velella_guide_on_air),
		style = LocalTextStyle.current.merge(color = palette.onAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold),
		maxLines = 1,
		modifier = Modifier
			.background(palette.accent, RoundedCornerShape(4.dp))
			.padding(horizontal = 6.dp, vertical = 1.dp),
	)
}

/** Watch (tunes to the channel) and Details; [watchFocusRequester] takes the focus when it enters the spotlight. */
@Composable
private fun LiveTvSpotlightButtons(item: BaseItemDto, watchFocusRequester: FocusRequester) {
	val context = LocalContext.current
	val playbackHelper = koinInject<PlaybackHelper>()
	val navigationRepository = koinInject<NavigationRepository>()
	val channelId = item.channelId

	Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
		// Icons only, the labels kept for TalkBack
		IconButton(
			onClick = { channelId?.let { playbackHelper.retrieveAndPlay(it, false, context) } },
			modifier = Modifier.focusRequester(watchFocusRequester),
		) {
			Icon(painterResource(R.drawable.ic_play), contentDescription = stringResource(R.string.lbl_play), modifier = Modifier.size(20.dp))
		}
		IconButton(onClick = { channelId?.let { navigationRepository.navigate(Destinations.channelDetails(item.id, it, item)) } }) {
			Icon(
				painterResource(R.drawable.ic_info),
				contentDescription = stringResource(R.string.velella_spotlight_details),
				modifier = Modifier.size(20.dp),
			)
		}
	}
}
