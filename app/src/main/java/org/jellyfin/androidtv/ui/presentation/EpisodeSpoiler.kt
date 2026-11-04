package org.jellyfin.androidtv.ui.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import org.jellyfin.androidtv.preference.EphyraPreferences
import org.jellyfin.androidtv.ui.composable.blurHashPainter
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.design.Tokens
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private const val SPOILER_BLUR_HEIGHT = 16

/**
 * Ephyra: whether [image] shows a scene of an episode the user has not started yet. Only the episode's own images count: a
 * series or season poster shown for the episode gives nothing away.
 */
fun isEpisodeSpoiler(item: BaseItemDto?, image: JellyfinImage): Boolean {
	if (item == null || item.type != BaseItemKind.EPISODE || image.item != item.id) return false
	val userData = item.userData ?: return true
	return !userData.played && userData.playbackPositionTicks <= 0
}

/** Ephyra: [isEpisodeSpoiler] when the "Blur unwatched episodes" setting is on. */
@Composable
fun hidesEpisodeSpoiler(item: BaseItemDto?, image: JellyfinImage): Boolean {
	val preferences = koinInject<EphyraPreferences>()
	return preferences[EphyraPreferences.spoilerBlur] && isEpisodeSpoiler(item, image)
}

/**
 * Ephyra: the blurred stand-in for a spoiler image, drawn from the image's BlurHash, so the picture itself is never downloaded
 * and no GPU blur is needed (Android 11 has none).
 */
@Composable
fun EpisodeSpoilerImage(image: JellyfinImage, aspectRatio: Float, modifier: Modifier = Modifier) {
	val blurHash = image.blurHash
	if (blurHash != null) {
		val size = IntSize((SPOILER_BLUR_HEIGHT * aspectRatio).roundToInt().coerceAtLeast(1), SPOILER_BLUR_HEIGHT)
		Image(
			painter = blurHashPainter(blurHash, size),
			contentDescription = null,
			contentScale = ContentScale.Crop,
			modifier = modifier,
		)
	} else {
		Box(modifier = modifier.background(Tokens.Color.colorBluegrey900))
	}
}
