package org.jellyfin.androidtv.ui.home.spotlight

import android.view.ViewTreeObserver
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.browsing.composable.inforow.COMMUNITY_RATING_SCALE
import org.jellyfin.androidtv.ui.browsing.composable.inforow.CRITIC_RATING_SCALE
import org.jellyfin.androidtv.ui.browsing.composable.inforow.InfoRowCommunityRating
import org.jellyfin.androidtv.ui.browsing.composable.inforow.InfoRowCriticRating
import org.jellyfin.androidtv.ui.composable.InfoRowGroups
import org.jellyfin.androidtv.ui.composable.InfoRowOutlinedBadge
import org.jellyfin.androidtv.ui.composable.modifier.BackdropPanSpec
import org.jellyfin.androidtv.ui.composable.modifier.backdropPan
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

// Leaves the first row's card labels, focused card included, above the bottom fade and the safe margin on a 540dp-high TV
private val SpotlightHeight = 226.dp
private val SpotlightInterval = 10.seconds
private const val SPOTLIGHT_FADE_MS = 240
private val LogoMaxWidth = 220.dp
private val LogoMaxHeight = 72.dp

// The backdrop covers the right two thirds and fades out at the top, towards the text and towards the rows, without blur
// (Android 11 has none)
private const val BACKDROP_WIDTH = 0.66f
// Positions of the backdrop fade's stops, as fractions of its width (horizontal) and height (vertical)
private const val FADE_H_SOFT = 0.25f
private const val FADE_H_FULL = 0.5f
private const val FADE_V_IN = 0.3f
private const val FADE_V_HOLD = 0.55f
private const val FADE_V_OUT = 0.8f
private val BackdropFadeHorizontal = Brush.horizontalGradient(
	0f to Color.Transparent,
	FADE_H_SOFT to Color.Black.copy(alpha = 0.6f),
	FADE_H_FULL to Color.Black,
)
private val BackdropFadeVertical = Brush.verticalGradient(
	0f to Color.Transparent,
	FADE_V_IN to Color.Black,
	FADE_V_HOLD to Color.Black,
	FADE_V_OUT to Color.Black.copy(alpha = 0.4f),
	1f to Color.Transparent,
)

/**
 * Velella: the spotlight at the top of the home screen (the `DetailHero` "feature" of the design in guide 5.1e): recent films and
 * series not watched yet, one at a time, with Play, Details and Favorite. It shows while the first home row is selected and
 * stops rotating while it has the focus.
 */
@Composable
fun HomeSpotlight(rowsFragment: HomeRowsFragment?, modifier: Modifier = Modifier) {
	val preferences = koinInject<VelellaPreferences>()
	if (!preferences[VelellaPreferences.homeSpotlight]) return

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
		) { shown ->
			// Velella: the pan alternates its side from title to title and each title's pan goes on through the crossfade
			SpotlightBackdrop(shown, api, BackdropPanSpec.direction(items.indexOfFirst { it.id == shown.id }))
		}

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
private fun SpotlightBackdrop(item: BaseItemDto, api: ApiClient, panDirection: Int) {
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
				.backdropPan(panDirection, key = item.id)
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
	val playFocusRequester = remember { FocusRequester() }

	Column(
		// Coming from the rows or the toolbar, the focus lands on Play
		modifier = modifier
			.width(520.dp)
			.focusProperties { onEnter = { playFocusRequester.requestFocus() } }
			.focusGroup(),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		SpotlightTexts(item)
		Spacer(Modifier.height(8.dp))
		SpotlightButtons(item, playFocusRequester)
	}
}

/** The overline, the title, the tagline and the info row of the spotlight. */
@Composable
private fun SpotlightTexts(item: BaseItemDto) {
	val resources = LocalResources.current

	Text(
		text = spotlightOverline(stringResource(R.string.velella_spotlight), item).uppercase(),
		style = LocalTextStyle.current.merge(
			color = LocalVelellaPalette.current.accent,
			fontSize = 11.sp,
			fontWeight = FontWeight.Bold,
			letterSpacing = 1.sp,
		),
	)
	Text(
		text = item.name.orEmpty(),
		style = LocalTextStyle.current.merge(
			color = LocalVelellaPalette.current.ink,
			fontSize = 28.sp,
			fontWeight = FontWeight.Bold,
			lineHeight = 34.sp,
		),
		maxLines = 1,
		overflow = TextOverflow.Ellipsis,
	)
	// Velella: the tagline, one line, when the title has one
	spotlightTagline(item)?.let { tagline ->
		Text(
			text = tagline,
			style = LocalTextStyle.current.merge(color = LocalVelellaPalette.current.inkMuted, fontSize = 14.sp),
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
	}
	// Ratings first, with the gold star and the Rotten Tomatoes logo, as in the details' info row; groups divided by thin
	// lines and the official rating in an outlined badge
	// Written out for one and two arguments (the runtime's) rather than spread
	val getString = { res: Int, args: Array<out Any> ->
		if (args.size > 1) resources.getString(res, args[0], args[1]) else resources.getString(res, args[0])
	}
	val runtime = spotlightRuntime(item, getString)
	InfoRowGroups(
		buildList<@Composable () -> Unit> {
			item.communityRating?.let { add { InfoRowCommunityRating(it / COMMUNITY_RATING_SCALE) } }
			item.criticRating?.let { add { InfoRowCriticRating(it / CRITIC_RATING_SCALE) } }
			item.productionYear?.let { add { SpotlightMetaText(it.toString()) } }
			runtime?.let { add { SpotlightMetaText(it) } }
			item.officialRating?.takeIf { it.isNotBlank() }?.let { add { InfoRowOutlinedBadge(it) } }
		}
	)
}

/** Play, Details and Favorite; [playFocusRequester] takes the focus when it enters the spotlight. */
@Composable
private fun SpotlightButtons(item: BaseItemDto, playFocusRequester: FocusRequester) {
	val context = LocalContext.current
	val api = koinInject<ApiClient>()
	val playbackHelper = koinInject<PlaybackHelper>()
	val navigationRepository = koinInject<NavigationRepository>()
	val itemMutationRepository = koinInject<ItemMutationRepository>()
	val scope = rememberCoroutineScope()
	// Favorite changes made here, until the next load
	val favorites = remember { mutableStateMapOf<UUID, Boolean>() }
	val favorite = favorites[item.id] ?: (item.userData?.isFavorite == true)

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
				contentDescription = stringResource(R.string.velella_spotlight_details),
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
				tint = if (favorite) LocalVelellaPalette.current.accent else LocalTextStyle.current.color,
			)
		}
	}
}

@Composable
private fun SpotlightMetaText(text: String) {
	Text(
		text = text,
		style = LocalTextStyle.current.merge(
			color = LocalVelellaPalette.current.inkMuted,
			fontSize = 14.sp,
			fontWeight = FontWeight.Medium,
		),
		maxLines = 1,
	)
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
