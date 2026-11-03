package org.jellyfin.androidtv.data.service

import android.content.Context
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale

/**
 * Ephyra: backdrops fill the screen, so decode them at screen size. A request without a target or size is decoded
 * at the image's original size (often 4K, about 33 MB per bitmap), and BackgroundService keeps every backdrop of an
 * item in memory. FILL matches the screens' crop; inexact precision only scales down, so smaller images are not enlarged.
 */
fun ImageRequest.Builder.backdropSize(context: Context): ImageRequest.Builder {
	val metrics = context.resources.displayMetrics
	return size(metrics.widthPixels, metrics.heightPixels).scale(Scale.FILL).precision(Precision.INEXACT)
}
