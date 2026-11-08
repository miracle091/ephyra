package org.jellyfin.androidtv.ui.home.spotlight

import android.view.ViewTreeObserver
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.preference.EphyraPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.browsing.composable.inforow.InfoRowCommunityRating
import org.jellyfin.androidtv.ui.browsing.composable.inforow.InfoRowCriticRating
import org.jellyfin.androidtv.ui.home.HomeRowsFragment
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.PlaybackHelper
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private val SpotlightHeight = 250.dp
private val SpotlightInterval = 10.seconds
private const val SPOTLIGHT_FADE_MS = 240
private val LogoMaxWidth = 220.dp
private val LogoMaxHeight = 72.dp

// The backdrop covers the right two thirds and fades out at the top, towards the text and towards the rows, without blur
// (Android 11 has none)
private const val BACKDROP_WIDTH = 0.66f
private val BackdropFadeHorizontal = Brush.horizontalGradient(
	0f to Color.Transparent,
	0.25f to Color.Black.copy(alpha = 0.6f),
	0.5f to Color.Black,
)
private val BackdropFadeVertical = Brush.verticalGradient(
	0f to Color.Transparent,
	0.3f to Color.Black,
	0.55f to Color.Black,
	0.8f to Color.Black.copy(alpha = 0.4f),
	1f to Color.Transparent,
)

/**
 * Ephyra: the spotlight at the top of the home screen (the `DetailHero` "feature" of the design in guide 5.1e): recent films and
 * series not watched yet, one at a time, with Play, Details and Favorite. It shows while the first home row is selected and
 * stops rotating while it has the focus.
 */
@Composable
fun HomeSpotlight(rowsFragment: HomeRowsFragment?, modifier: Modifier = Modifier) {
	val preferences = koinInject<EphyraPreferences>()
	if (!preferences[EphyraPreferences.homeSpotlight]) return

	val api = koinInject<ApiClient>()
	val items by produceState(emptyList<BaseItemDto>(), api) { value = loadSpotlightItems(api) }
	val firstRowSelected by rememberFirstRowSelected(rowsFragment)
	// Kept above the early return, so the rotation goes on where it was when the spotlight shows again
	var index by remember { mutableIntStateOf(0) }
	if (items.isEmpty() || !firstRowSelected) return

	var focused by remember { mutableStateOf(false) }
	LaunchedEffect(items, focused) {
		while (!focused && items.size > 1) {
			delay(SpotlightInterval)
			index = (index + 1) % items.size
		}
	}

	val item = items[index % items.size]
	Box(
		modifier = modifier
			.fillMaxWidth()
			.height(SpotlightHeight)
	) {
		Crossfade(
			targetState = item,
			animationSpec = tween(SPOTLIGHT_FADE_MS),
			label = "SpotlightBackdrop",
			modifier = Modifier
				.align(Alignment.TopEnd)
				.fillMaxWidth(BACKDROP_WIDTH)
				.fillMaxHeight(),
		) { shown -> SpotlightBackdrop(shown, api) }

		SpotlightInfo(
			item = item,
			modifier = Modifier
				// Left aligned, centred on the backdrop's height
				.align(Alignment.CenterStart)
				.padding(start = 32.dp)
				.onFocusChanged { focused = it.hasFocus },
		)
	}
}

@Composable
private fun SpotlightBackdrop(item: BaseItemDto, api: ApiClient) {
	val image = item.itemBackdropImages.firstOrNull() ?: return
	val maxWidth = with(LocalDensity.current) { 640.dp.roundToPx() }
	val logoMaxWidth = with(LocalDensity.current) { LogoMaxWidth.roundToPx() }
	Box(Modifier.fillMaxSize()) {
		AsyncImage(
			model = image.getUrl(api, maxWidth = maxWidth),
			contentDescription = null,
			contentScale = ContentScale.Crop,
			modifier = Modifier
				.fillMaxSize()
				.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
				.drawWithContent {
					drawContent()
					drawRect(BackdropFadeHorizontal, blendMode = BlendMode.DstIn)
					drawRect(BackdropFadeVertical, blendMode = BlendMode.DstIn)
				},
		)

		// The title's logo, in the bottom right corner of the backdrop, when the server has one
		item.itemImages[ImageType.LOGO]?.let { logo ->
			AsyncImage(
				model = logo.getUrl(api, maxWidth = logoMaxWidth),
				contentDescription = null,
				contentScale = ContentScale.Fit,
				alignment = Alignment.BottomEnd,
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(end = 32.dp, bottom = 24.dp)
					.size(width = LogoMaxWidth, height = LogoMaxHeight),
			)
		}
	}
}

@Composable
private fun SpotlightInfo(item: BaseItemDto, modifier: Modifier = Modifier) {
	val context = LocalContext.current
	val resources = LocalResources.current
	val api = koinInject<ApiClient>()
	val playbackHelper = koinInject<PlaybackHelper>()
	val navigationRepository = koinInject<NavigationRepository>()
	val itemMutationRepository = koinInject<ItemMutationRepository>()
	val scope = rememberCoroutineScope()
	// Favorite changes made here, until the next load
	val favorites = remember { mutableStateMapOf<UUID, Boolean>() }
	val favorite = favorites[item.id] ?: (item.userData?.isFavorite == true)
	val playFocusRequester = remember { FocusRequester() }

	Column(
		// Coming from the rows or the toolbar, the focus lands on Play
		modifier = modifier
			.width(520.dp)
			.focusProperties { onEnter = { playFocusRequester.requestFocus() } }
			.focusGroup(),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		Text(
			text = spotlightOverline(stringResource(R.string.ephyra_spotlight), item).uppercase(),
			fontSize = 11.sp,
			fontWeight = FontWeight.Bold,
			letterSpacing = 1.sp,
			color = LocalEphyraPalette.current.accent,
		)
		Text(
			text = item.name.orEmpty(),
			fontSize = 28.sp,
			lineHeight = 34.sp,
			fontWeight = FontWeight.Bold,
			color = LocalEphyraPalette.current.ink,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
		// Ratings first, with the gold star and the Rotten Tomatoes logo, as in the details' info row
		Row(
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.height(IntrinsicSize.Min),
		) {
			item.communityRating?.let { InfoRowCommunityRating(it / 10f) }
			item.criticRating?.let { InfoRowCriticRating(it / 100f) }
			Text(
				text = spotlightMeta(item) { res, args -> resources.getString(res, *args) },
				fontSize = 14.sp,
				fontWeight = FontWeight.Medium,
				color = LocalEphyraPalette.current.inkMuted,
				maxLines = 1,
			)
		}
		Spacer(Modifier.height(8.dp))
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
			IconButton(
				// A series starts from its next up episode
				onClick = { scope.launch { playbackHelper.retrieveAndPlay(loadSpotlightPlayTarget(api, item), false, context) } },
				modifier = Modifier.focusRequester(playFocusRequester),
			) {
				Icon(
					painterResource(R.drawable.ic_play),
					contentDescription = stringResource(R.string.lbl_play),
					modifier = Modifier.size(20.dp),
				)
			}
			IconButton(onClick = { navigationRepository.navigate(Destinations.itemDetails(item.id)) }) {
				Icon(
					painterResource(R.drawable.ic_info),
					contentDescription = stringResource(R.string.ephyra_spotlight_details),
					modifier = Modifier.size(20.dp),
				)
			}
			IconButton(onClick = {
				scope.launch {
					try {
						favorites[item.id] = itemMutationRepository.setFavorite(item.id, !favorite).isFavorite
					} catch (error: CancellationException) {
						throw error
					} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
						Timber.w(error, "Could not change the favorite from the home spotlight")
					}
				}
			}) {
				Icon(
					painterResource(R.drawable.ic_heart),
					contentDescription = stringResource(R.string.lbl_favorite),
					modifier = Modifier.size(20.dp),
					// A favorite shows in the theme's accent instead of red
					tint = if (favorite) LocalEphyraPalette.current.accent else LocalTextStyle.current.color,
				)
			}
		}
	}
}

/** Whether the first row of [rowsFragment] is selected, watched through focus changes (Leanback has no Compose state). */
@Composable
private fun rememberFirstRowSelected(rowsFragment: HomeRowsFragment?) = remember { mutableStateOf(true) }.also { state ->
	DisposableEffect(rowsFragment) {
		val view = rowsFragment?.view
		val listener = ViewTreeObserver.OnGlobalFocusChangeListener { _, _ ->
			// The row selection settles after the focus moves
			view?.post { state.value = rowsFragment.selectedPosition <= 0 }
		}
		view?.viewTreeObserver?.addOnGlobalFocusChangeListener(listener)
		onDispose { view?.viewTreeObserver?.removeOnGlobalFocusChangeListener(listener) }
	}
}
