package org.jellyfin.androidtv.ui.home.spotlight

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
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

// Leaves the first row's card labels, focused card included, above the bottom fade and the safe margin on a 540dp-high TV
private val SpotlightHeight = 226.dp
// The backdrop runs behind the toolbar too, so it covers the top 60% of a 540dp-high TV
private val ToolbarHeight = 95.dp
private val SpotlightBackdropHeight = ToolbarHeight + SpotlightHeight
private const val SPOTLIGHT_FADE_MS = 240
private val LogoMaxWidth = 320.dp
private val LogoMaxHeight = 72.dp
private val BackdropMaxWidth = 960.dp

// The backdrop spans the screen and fades towards the text on the left and towards the rows at the bottom, without blur
// (Android 11 has none); the top stays a little darker under the toolbar
private const val FADE_H_SOFT = 0.4f
private const val FADE_H_FULL = 0.8f
private const val FADE_V_TOP = 0.25f
private const val FADE_V_HOLD = 0.6f
// The veil over the backdrop: strong across the text column (560dp from 32dp, about 60% of a 960dp-wide TV) and fading after
// it, and over the toolbar's band on top
private const val SCRIM_SIDE = 0.85f
private const val SCRIM_SIDE_HOLD = 0.6f
private const val SCRIM_SIDE_HELD = 0.7f
private const val SCRIM_SIDE_END = 0.85f
private const val SCRIM_TOP = 0.8f
private const val SCRIM_TOP_HOLD = 0.2f
private const val SCRIM_TOP_HELD = 0.6f
private const val SCRIM_TOP_END = 0.4f
private val BackdropFadeHorizontal = Brush.horizontalGradient(
	0f to Color.Black.copy(alpha = 0.1f),
	FADE_H_SOFT to Color.Black.copy(alpha = 0.3f),
	FADE_H_FULL to Color.Black,
)
private val BackdropFadeVertical = Brush.verticalGradient(
	0f to Color.Black.copy(alpha = 0.55f),
	FADE_V_TOP to Color.Black,
	FADE_V_HOLD to Color.Black,
	1f to Color.Transparent,
)

/** Velella: the backdrop of the item showing, across the top of the screen behind the toolbar and the spotlight's text. */
@Composable
fun SpotlightBackdrop(state: SpotlightState?, modifier: Modifier = Modifier) {
	val item = state?.item ?: return
	val api = koinInject<ApiClient>()
	val bg = LocalVelellaPalette.current.bg
	Crossfade(
		targetState = item,
		animationSpec = tween(SPOTLIGHT_FADE_MS),
		label = "SpotlightBackdrop",
		modifier = modifier
			.fillMaxWidth()
			.height(SpotlightBackdropHeight)
			// Velella: a veil in the theme's background behind the text on the left and the toolbar on top, so the overview,
			// the clock and the icons stay readable over a bright image
			.drawWithContent {
				drawContent()
				drawRect(
					Brush.horizontalGradient(
						0f to bg.copy(alpha = SCRIM_SIDE),
						SCRIM_SIDE_HOLD to bg.copy(alpha = SCRIM_SIDE_HELD),
						SCRIM_SIDE_END to bg.copy(alpha = 0f),
					)
				)
				drawRect(
					Brush.verticalGradient(
						0f to bg.copy(alpha = SCRIM_TOP),
						SCRIM_TOP_HOLD to bg.copy(alpha = SCRIM_TOP_HELD),
						SCRIM_TOP_END to bg.copy(alpha = 0f),
					)
				)
			},
	) { shown ->
		// Velella: the pan alternates its side from title to title and each title's pan goes on through the crossfade
		SpotlightBackdropImage(shown, api, BackdropPanSpec.direction(state.items.indexOfFirst { it.id == shown.id }))
	}
}

/** Velella: the home spotlight's text and buttons, under the toolbar, above the rows. */
@Composable
fun HomeSpotlight(state: SpotlightState?, modifier: Modifier = Modifier) {
	SpotlightFrame(state, modifier) { item, page, pages, infoModifier -> SpotlightInfo(item, page, pages, infoModifier) }
}

/**
 * Velella: the room of a spotlight under the toolbar, above the rows, with the [info] of the item showing (its page among the
 * pages) left aligned and centred on its height. Nothing while no item shows.
 */
@Composable
internal fun SpotlightFrame(
	state: SpotlightState?,
	modifier: Modifier = Modifier,
	info: @Composable (item: BaseItemDto, page: Int, pages: Int, modifier: Modifier) -> Unit,
) {
	val item = state?.item ?: return
	Box(
		modifier = modifier
			.fillMaxWidth()
			.height(SpotlightHeight)
	) {
		info(
			item,
			state.index % state.items.size,
			state.items.size,
			Modifier
				.align(Alignment.CenterStart)
				.padding(start = 32.dp)
				.onFocusChanged { state.focused = it.hasFocus },
		)
	}
}

@Composable
private fun SpotlightBackdropImage(item: BaseItemDto, api: ApiClient, panDirection: Int) {
	// A programme on air often has no backdrop, only its thumbnail or primary image
	val image = item.itemBackdropImages.firstOrNull() ?: item.itemImages[ImageType.THUMB] ?: item.itemImages[ImageType.PRIMARY] ?: return
	val maxWidth = with(LocalDensity.current) { BackdropMaxWidth.roundToPx() }
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
}

@Composable
private fun SpotlightInfo(item: BaseItemDto, page: Int, pages: Int, modifier: Modifier = Modifier) {
	val playFocusRequester = remember { FocusRequester() }

	Column(
		// Coming from the rows or the toolbar, the focus lands on Play
		modifier = modifier
			.width(560.dp)
			.focusProperties { onEnter = { playFocusRequester.requestFocus() } }
			.focusGroup(),
		verticalArrangement = Arrangement.spacedBy(6.dp),
	) {
		SpotlightTexts(item)
		Spacer(Modifier.height(4.dp))
		Row(verticalAlignment = Alignment.CenterVertically) {
			SpotlightButtons(item, playFocusRequester)
			Spacer(Modifier.width(24.dp))
			SpotlightDashes(page, pages)
		}
	}
}

/** The title (its logo when the server has one), the info row and the overview of the spotlight. */
@Composable
private fun SpotlightTexts(item: BaseItemDto) {
	val resources = LocalResources.current
	val api = koinInject<ApiClient>()
	val logoMaxWidth = with(LocalDensity.current) { LogoMaxWidth.roundToPx() }

	val logo = item.itemImages[ImageType.LOGO]
	if (logo != null) {
		AsyncImage(
			model = logo.getUrl(api, maxWidth = logoMaxWidth),
			contentDescription = item.name,
			contentScale = ContentScale.Fit,
			alignment = Alignment.CenterStart,
			modifier = Modifier.size(width = LogoMaxWidth, height = LogoMaxHeight),
		)
	} else {
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
	}
	// Ratings first, with the gold star and the Rotten Tomatoes logo, as in the details' info row; groups divided by thin
	// lines, the genres joined by dots and the official rating in an outlined badge
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
			spotlightGenres(item)?.let { add { SpotlightMetaText(it) } }
			item.officialRating?.takeIf { it.isNotBlank() }?.let { add { InfoRowOutlinedBadge(it) } }
		}
	)
	// Velella: the overview, up to three lines, or the tagline when the title has no overview
	spotlightOverview(item)?.let { overview ->
		Text(
			text = overview,
			style = LocalTextStyle.current.merge(color = LocalVelellaPalette.current.inkMuted, fontSize = 14.sp, lineHeight = 19.sp),
			maxLines = 3,
			overflow = TextOverflow.Ellipsis,
		)
	}
}

/** One dash per item of the spotlight, the one showing longer and in the accent colour; nothing for a single item. */
@Composable
internal fun SpotlightDashes(page: Int, pages: Int) {
	if (pages < 2) return
	val palette = LocalVelellaPalette.current
	Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
		repeat(pages) { dash ->
			val current = dash == page
			Box(
				Modifier
					.size(width = if (current) 24.dp else 12.dp, height = 3.dp)
					.background(if (current) palette.accent else palette.ink.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
			)
		}
	}
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
		// Icons only, the labels kept for TalkBack
		IconButton(
			// A series starts from its next up episode
			onClick = { scope.launch { playbackHelper.retrieveAndPlay(loadSpotlightPlayTarget(api, item), false, context) } },
			modifier = Modifier.focusRequester(playFocusRequester),
		) {
			Icon(painterResource(R.drawable.ic_play), contentDescription = stringResource(R.string.lbl_play), modifier = Modifier.size(20.dp))
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
internal fun SpotlightMetaText(text: String) {
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
