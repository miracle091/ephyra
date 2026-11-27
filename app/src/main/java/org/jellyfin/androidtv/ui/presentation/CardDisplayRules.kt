package org.jellyfin.androidtv.ui.presentation

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

// Velella: a landscape card is 130dp tall where a portrait one is 150dp
private const val LANDSCAPE_HEIGHT_RATIO = 130f / 150f

// Velella: aspect ratios below this are treated as missing
private const val MIN_ASPECT_RATIO = 0.1f

// Items narrower than this are shown as a square when they are music or people
private const val SQUARE_ASPECT_THRESHOLD = 0.8f

/**
 * The aspect ratio of a card for [imageType]: banner and thumb cards have their own fixed ratio, any other type uses [fallback].
 */
internal fun imageTypeAspectRatio(imageType: ImageType, fallback: Float): Float = when (imageType) {
	ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
	ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
	else -> fallback
}

/**
 * The aspect ratio of the card of a regular item before the image type is applied: wide for the items shown with a parent
 * thumbnail, episodes and libraries, else the primary image's.
 */
internal fun baseItemDefaultAspectRatio(item: BaseItemDto?, preferParentThumb: Boolean): Float {
	val primaryAspectRatio = item?.primaryImageAspectRatio?.toFloat()
	val aspectRatio16x9 = ImageHelper.ASPECT_RATIO_16_9.toFloat()
	val hasParentThumb = item?.parentThumbItemId != null || item?.seriesThumbImageTag != null
	val isEpisode = item?.type == BaseItemKind.EPISODE

	return when {
		preferParentThumb && hasParentThumb -> aspectRatio16x9
		isEpisode && primaryAspectRatio != null -> primaryAspectRatio
		isEpisode && hasParentThumb -> aspectRatio16x9
		item?.type == BaseItemKind.USER_VIEW -> aspectRatio16x9
		else -> primaryAspectRatio ?: ImageHelper.ASPECT_RATIO_7_9.toFloat()
	}
}

/**
 * The drawable shown for an item of [kind] that has no picture.
 */
internal fun itemKindIcon(kind: BaseItemKind?): Int = when (kind) {
	BaseItemKind.AUDIO, BaseItemKind.MUSIC_ALBUM -> R.drawable.ic_music_album
	BaseItemKind.PERSON, BaseItemKind.MUSIC_ARTIST -> R.drawable.ic_user
	BaseItemKind.SEASON, BaseItemKind.SERIES, BaseItemKind.EPISODE -> R.drawable.ic_tv

	BaseItemKind.COLLECTION_FOLDER,
	BaseItemKind.USER_VIEW,
	BaseItemKind.FOLDER,
	BaseItemKind.GENRE,
	BaseItemKind.MUSIC_GENRE,
	BaseItemKind.PHOTO_ALBUM,
	BaseItemKind.PLAYLIST -> R.drawable.ic_folder

	BaseItemKind.PHOTO -> R.drawable.ic_photo
	else -> R.drawable.ic_clapperboard
}

/**
 * The aspect ratio of the card of an item of [kind], given the [baseAspectRatio] worked out for the image type.
 */
internal fun itemKindAspectRatio(
	kind: BaseItemKind?,
	baseAspectRatio: Float,
	imageType: ImageType,
	uniformAspect: Boolean,
	preferSeriesPoster: Boolean,
): Float {
	val poster = ImageHelper.ASPECT_RATIO_2_3.toFloat()
	val wide = ImageHelper.ASPECT_RATIO_16_9.toFloat()

	return when (kind) {
		BaseItemKind.AUDIO,
		BaseItemKind.MUSIC_ALBUM,
		BaseItemKind.PERSON,
		BaseItemKind.MUSIC_ARTIST -> if (uniformAspect || baseAspectRatio < SQUARE_ASPECT_THRESHOLD) 1f else baseAspectRatio

		BaseItemKind.SEASON,
		BaseItemKind.SERIES,
		BaseItemKind.MOVIE,
		BaseItemKind.VIDEO -> if (imageType == ImageType.POSTER) poster else baseAspectRatio

		BaseItemKind.EPISODE -> if (preferSeriesPoster) poster else wide
		BaseItemKind.COLLECTION_FOLDER, BaseItemKind.USER_VIEW -> wide
		else -> baseAspectRatio
	}
}

/**
 * The aspect ratio a card is drawn with: the one of its [configAspectRatio], else the one of its image, else square. Values
 * below [MIN_ASPECT_RATIO] count as missing.
 */
internal fun resolveAspectRatio(configAspectRatio: Float, imageAspectRatio: Float?): Float =
	configAspectRatio.takeIf { it >= MIN_ASPECT_RATIO }
		?: imageAspectRatio?.takeIf { it >= MIN_ASPECT_RATIO }
		?: 1f

/**
 * The size of a card: [staticHeight] when the item asks for a static height, else the presenter's [cardHeight] when set
 * (landscape cards keep their 130:150 ratio to it), else 130dp for landscape and 150dp for portrait cards.
 */
internal fun cardSize(aspectRatio: Float, itemStaticHeight: Boolean, staticHeight: Int, cardHeight: Int?): DpSize {
	val fixedHeight = cardHeight?.dp?.let { if (aspectRatio > 1f) it * LANDSCAPE_HEIGHT_RATIO else it }

	return when (itemStaticHeight) {
		false if fixedHeight != null -> DpSize(fixedHeight * aspectRatio, fixedHeight)
		true -> DpSize(staticHeight.dp * aspectRatio, staticHeight.dp)
		false if (aspectRatio > 1f) -> DpSize(130.dp * aspectRatio, 130.dp)
		else -> DpSize(150.dp * aspectRatio, 150.dp)
	}
}
