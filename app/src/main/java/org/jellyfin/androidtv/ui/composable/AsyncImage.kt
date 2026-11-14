package org.jellyfin.androidtv.ui.composable

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import org.jellyfin.androidtv.ui.AsyncImageView
import org.jellyfin.androidtv.util.BlurHashDecoder

private const val BLUR_HASH_RESOLUTION = 32

/** The image an [AsyncImage] shows: loaded from [url] while the [blurHash] is shown as a placeholder. */
data class AsyncImageSource(
	val url: String? = null,
	val blurHash: String? = null,
)

@Composable
fun AsyncImage(
	source: AsyncImageSource,
	modifier: Modifier = Modifier,
	placeholder: Drawable? = null,
	aspectRatio: Float = 1f,
	scaleType: ImageView.ScaleType? = null,
) {
	// Only the important properties are added to the state
	var state by remember { mutableStateOf<AsyncImageSource?>(null) }

	AndroidView(
		modifier = modifier,
		factory = { context ->
			AsyncImageView(context).also { view ->
				view.adjustViewBounds = true
				view.scaleType = scaleType ?: ImageView.ScaleType.FIT_CENTER
			}
		},
		update = { view ->
			val compositionState = source
			if (state != compositionState) {
				state = compositionState

				view.load(
					url = compositionState.url,
					blurHash = compositionState.blurHash,
					placeholder = placeholder,
					aspectRatio = aspectRatio.toDouble(),
					blurHashResolution = BLUR_HASH_RESOLUTION,
				)
			}
		},
	)
}

@Composable
fun blurHashPainter(
	blurHash: String,
	size: IntSize,
	punch: Float = 1f,
): Painter = remember(blurHash, size, punch) {
	val bitmap = BlurHashDecoder.decode(
		blurHash = blurHash,
		width = size.width,
		height = size.height,
		punch = punch,
	)

	BitmapPainter(requireNotNull(bitmap).asImageBitmap())
}
