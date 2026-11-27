package org.jellyfin.androidtv.ui.presentation

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.leanback.widget.Presenter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.AsyncImageSource
import org.jellyfin.androidtv.ui.composable.item.ArtToneCover
import org.jellyfin.androidtv.ui.composable.item.CardPlaceholder
import org.jellyfin.androidtv.ui.composable.item.ItemCard
import org.jellyfin.androidtv.ui.composable.item.ItemCardBaseItemOverlay
import org.jellyfin.androidtv.ui.composable.item.ItemPreview
import org.jellyfin.androidtv.ui.composable.item.cardFocusRing
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowType
import org.jellyfin.androidtv.ui.home.SeeAllButton
import org.jellyfin.androidtv.ui.itemhandling.GridButtonBaseRowItem
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.getActivity
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.compose.koinInject

// Velella: the design system's card overlay shade, rgba(5, 8, 20)
private val CARD_SHADE = Color(red = 5, green = 8, blue = 20)

private const val DEFAULT_CARD_HEIGHT_DP = 150

// Velella: where the card overlay shade starts and how opaque it is at the bottom
private const val SHADE_START = 0.4f
private const val SHADE_END_ALPHA = 0.86f

class CardPresenter(
	val showInfo: Boolean,
	val imageType: ImageType,
	val staticHeight: Int,
	val uniformAspect: Boolean,
) : Presenter() {
	constructor(showInfo: Boolean, imageType: ImageType, staticHeight: Int) : this(showInfo, imageType, staticHeight, false)
	constructor(showInfo: Boolean, staticHeight: Int) : this(showInfo, ImageType.POSTER, staticHeight)
	constructor(showInfo: Boolean) : this(showInfo, DEFAULT_CARD_HEIGHT_DP)
	constructor() : this(true)

	/** Velella: episodes show their meta line (runtime, rating, end time) under the title instead of their season and number. */
	var episodeTiming = false

	/**
	 * Velella: when set, the height in dp of portrait cards, whatever the item asks for; landscape cards keep their usual 130:150
	 * ratio to it.
	 */
	var cardHeight: Int? = null

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = ComposeView(parent.context).apply {
			setParentCompositionContext(parent.findViewTreeCompositionContext())
			setViewTreeLifecycleOwner(parent.findViewTreeLifecycleOwner())
			setViewTreeSavedStateRegistryOwner(parent.findViewTreeSavedStateRegistryOwner())
			isFocusable = true
			isFocusableInTouchMode = true

			setOnLongClickListener {
				context.getActivity()?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU)) ?: false
			}
		}

		return CardViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		if (viewHolder !is CardViewHolder) return
		if (item !is BaseRowItem) return

		viewHolder.bind(item)
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		if (viewHolder !is CardViewHolder) return

		viewHolder.unbind()
	}

	private inner class CardViewHolder(composeView: ComposeView) : ViewHolder(composeView) {
		private val _item = MutableStateFlow<BaseRowItem?>(null)
		private val _focused = MutableStateFlow(false)

		init {
			composeView.setContent {
				val item by _item.collectAsState()
				val focused by _focused.collectAsState()

				CardViewHolderContent(
					item = item,
					focused = focused,
					presenter = this@CardPresenter,
				)
			}

			_focused.value = view.isFocused
			composeView.onFocusChangeListener = { _, focused -> _focused.value = focused }
		}

		fun bind(item: BaseRowItem) {
			_item.value = item
			_focused.value = view.isFocused
		}

		fun unbind() {
			_item.value = null
			_focused.value = false
		}
	}
}

private data class BaseRowItemDisplayConfig(
	val image: JellyfinImage?,
	val iconRes: Int,
	val aspectRatio: Float,
	val overrideShowInfo: Boolean? = null,
	val scaleType: ImageView.ScaleType? = null,
)

private fun BaseRowItem.getBaseItemDisplayConfig(imageType: ImageType, uniformAspect: Boolean): BaseRowItemDisplayConfig {
	val preferSeriesPoster = this is BaseItemDtoBaseRowItem && preferSeriesPoster
	val kind = baseItem?.type
	val imageTypeAspectRatio = imageTypeAspectRatio(imageType, baseItemDefaultAspectRatio(baseItem, preferParentThumb))

	return BaseRowItemDisplayConfig(
		aspectRatio = itemKindAspectRatio(kind, imageTypeAspectRatio, imageType, uniformAspect, preferSeriesPoster),
		image = getImage(imageType),
		iconRes = itemKindIcon(kind),
		overrideShowInfo = if (kind == BaseItemKind.EPISODE && !preferSeriesPoster) true else null,
	)
}

private fun BaseRowItem.getDisplayConfig(imageType: ImageType, uniformAspect: Boolean): BaseRowItemDisplayConfig = when (baseRowType) {
	BaseRowType.BaseItem -> getBaseItemDisplayConfig(imageType, uniformAspect)

	BaseRowType.LiveTvChannel -> BaseRowItemDisplayConfig(
		aspectRatio = imageTypeAspectRatio(imageType, baseItem?.primaryImageAspectRatio?.toFloat() ?: 1f),
		image = getImage(imageType),
		scaleType = ImageView.ScaleType.FIT_CENTER,
		iconRes = R.drawable.ic_tv,
	)

	BaseRowType.LiveTvProgram -> BaseRowItemDisplayConfig(
		aspectRatio = imageTypeAspectRatio(imageType, baseItem?.primaryImageAspectRatio?.toFloat() ?: ImageHelper.ASPECT_RATIO_7_9.toFloat()),
		image = getImage(imageType),
		iconRes = R.drawable.ic_tv,
		overrideShowInfo = true,
	)

	BaseRowType.LiveTvRecording -> BaseRowItemDisplayConfig(
		aspectRatio = imageTypeAspectRatio(imageType, baseItem?.primaryImageAspectRatio?.toFloat() ?: ImageHelper.ASPECT_RATIO_7_9.toFloat()),
		image = getImage(imageType),
		iconRes = R.drawable.ic_tv,
	)

	BaseRowType.SeriesTimer -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_16_9.toFloat(),
		iconRes = R.drawable.ic_tv_timer,
		image = getImage(imageType),
		overrideShowInfo = true,
	)

	BaseRowType.Person -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_7_9.toFloat(),
		image = getImage(imageType),
		iconRes = R.drawable.ic_user,
	)

	BaseRowType.Chapter -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_16_9.toFloat(),
		image = getImage(imageType),
		iconRes = R.drawable.ic_clapperboard,
	)

	BaseRowType.GridButton -> {
		val button = (this as? GridButtonBaseRowItem)?.gridButton
		BaseRowItemDisplayConfig(
			// Velella: a See all card takes the shape of the card before it
			aspectRatio = (button as? SeeAllButton)?.neighbour?.getDisplayConfig(imageType, uniformAspect)?.aspectRatio
				?: ImageHelper.ASPECT_RATIO_7_9.toFloat(),
			image = getImage(imageType),
			iconRes = button?.iconRes ?: R.drawable.ic_clapperboard,
		)
	}
}

@Composable
@Stable
private fun CardViewHolderContent(
	item: BaseRowItem?,
	focused: Boolean,
	// Velella: the presenter carries the card options (info, image type, heights, episode timing)
	presenter: CardPresenter,
) {
	val imageType = presenter.imageType
	val uniformAspect = presenter.uniformAspect
	val episodeTiming = presenter.episodeTiming
	val context = LocalContext.current

	val title = remember(item, context) { item?.getCardName(context) }
	val subtitle = remember(item, context) { item?.getSubText(context) }
	// Velella: the end time is worked out when the card is bound, so it is fresh each time the screen is shown
	val meta = remember(item, context, episodeTiming) {
		item?.baseItem?.takeIf { episodeTiming && it.type == BaseItemKind.EPISODE }?.episodeMetaLine(context)
	}
	val displayConfig = remember(item, imageType, uniformAspect) { item?.getDisplayConfig(imageType, uniformAspect) }
	if (item == null || displayConfig == null) return

	val aspectRatio = resolveAspectRatio(displayConfig.aspectRatio, displayConfig.image?.aspectRatio)
	val size = cardSize(aspectRatio, item.staticHeight, presenter.staticHeight, presenter.cardHeight)
	val usePreview = displayConfig.overrideShowInfo ?: presenter.showInfo

	val card = @Composable {
		ItemCard(
			image = { CardImage(item, title, displayConfig, size, aspectRatio) },
			overlay = { CardOverlay(item, title, !usePreview && item.showCardInfoOverlay, focused) },
			modifier = Modifier
				.size(size)
				.cardFocusRing( // Velella
					focused = focused,
					ring = LocalVelellaPalette.current.accent,
					shape = JellyfinTheme.shapes.medium,
				)
		)
	}

	if (usePreview) CardPreview(card, title, subtitle, meta, focused) else card()
}

private fun focusedMarquee(focused: Boolean): Modifier = if (focused) Modifier.basicMarquee(
	iterations = Int.MAX_VALUE,
	initialDelayMillis = 0,
) else Modifier

@Composable
private fun CardImage(
	item: BaseRowItem,
	title: String?,
	displayConfig: BaseRowItemDisplayConfig,
	size: DpSize,
	aspectRatio: Float,
) {
	val image = displayConfig.image

	CardPlaceholder() // Velella
	val library = item.baseItem?.takeIf(::isLibraryCard) // Velella
	if (library != null) {
		// Velella: Live TV shows a programme on air, with the TV icon when none has a picture
		val iconRes = if (isLiveTvLibraryCard(library)) R.drawable.ic_tv else displayConfig.iconRes
		LibraryCardImage(library, title, iconRes, size, aspectRatio)
	} else if (image != null && hidesEpisodeSpoiler(item.baseItem, image)) {
		EpisodeSpoilerImage(image, aspectRatio, Modifier.fillMaxSize()) // Velella
	} else if (image != null) {
		val api = koinInject<ApiClient>()
		val localDensity = LocalDensity.current
		AsyncImage(
			source = AsyncImageSource(
				url = image.getUrl(
					api,
					maxWidth = with(localDensity) { size.width.roundToPx() },
					maxHeight = with(localDensity) { size.height.roundToPx() },
				),
				blurHash = image.blurHash,
			),
			aspectRatio = aspectRatio,
			scaleType = displayConfig.scaleType ?: ImageView.ScaleType.CENTER_CROP,
			modifier = Modifier
				.fillMaxSize()
		)
	} else if (item is GridButtonBaseRowItem && item.gridButton.imageRes != null) {
		Image(
			painter = painterResource(item.gridButton.imageRes),
			contentDescription = null,
			modifier = Modifier
				.fillMaxSize()
		)
	} else {
		// Velella: a button with its own icon, as See all, shows it on the colour tone
		ArtToneCover(title, displayConfig.iconRes, alwaysShowIcon = (item as? GridButtonBaseRowItem)?.gridButton?.iconRes != null)
	}
}

@Composable
private fun CardOverlay(item: BaseRowItem, title: String?, showInfo: Boolean, focused: Boolean) {
	// Velella: the design system's card overlay, a bottom shade under the bold title, instead of a grey box
	if (showInfo && title != null) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Brush.verticalGradient(SHADE_START to CARD_SHADE.copy(alpha = 0f), 1f to CARD_SHADE.copy(alpha = SHADE_END_ALPHA))),
		)
	}
	item.baseItem?.let { baseItem ->
		ItemCardBaseItemOverlay(
			item = baseItem,
			footer = {
				if (showInfo && title != null) CardFooterTitle(title, focused)
			}
		)
	}
}

@Composable
private fun CardFooterTitle(title: String, focused: Boolean) {
	Text(
		text = title,
		maxLines = 1,
		overflow = TextOverflow.Ellipsis,
		style = LocalTextStyle.current.merge(
			color = LocalVelellaPalette.current.ink,
			fontSize = 18.sp,
			fontWeight = FontWeight.Bold,
			lineHeight = 22.sp,
		),
		modifier = Modifier
			.fillMaxWidth()
			.then(focusedMarquee(focused))
			.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
	)
}

@Composable
private fun CardPreview(
	card: @Composable () -> Unit,
	title: String?,
	subtitle: String?,
	meta: String?,
	focused: Boolean,
) {
	val focusModifier = focusedMarquee(focused)

	ItemPreview(
		card = { card() },
		title = title?.let { text ->
			{
				Text(
					text = text,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					style = LocalTextStyle.current.merge(textAlign = TextAlign.Center),
					// Velella: the full card width, so the title centres like the line under it
					modifier = Modifier
						.fillMaxWidth()
						.then(focusModifier),
				)
			}
		},
		subtitle = if (subtitle == null && meta == null) null else {
			{
				// Velella: the episode's meta line goes under its name
				Column {
					listOfNotNull(subtitle, meta).forEach { text ->
						Text(
							text = text,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
							style = LocalTextStyle.current.merge(textAlign = TextAlign.Center),
							modifier = Modifier
								.fillMaxWidth()
								.then(focusModifier),
						)
					}
				}
			}
		},
	)
}
