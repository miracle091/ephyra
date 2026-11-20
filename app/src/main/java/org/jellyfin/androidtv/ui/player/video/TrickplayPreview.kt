package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.maxBitmapSize
import coil3.request.transformations
import coil3.size.Dimension
import coil3.size.Size
import org.jellyfin.androidtv.integration.dream.formatPlaybackTime
import org.jellyfin.androidtv.ui.base.LocalEphyraPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.util.coil.SubsetTransformation
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.trickPlayApi
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.compose.koinInject
import kotlin.time.Duration

private val PreviewWidth = 240.dp
private val PreviewGap = 12.dp
private val PreviewShape = RoundedCornerShape(8.dp)

/**
 * Ephyra: the trickplay thumbnail of [position] with its time, drawn above the seek bar at [fraction] of its width while
 * scrubbing, as the legacy player's thumbnails row did. It takes no room in the layout: it is placed over the video, kept
 * inside the seek bar's width. Nothing shows when the item has no trickplay thumbnails for [mediaSourceId].
 */
@Composable
fun TrickplayPreview(
	item: BaseItemDto?,
	mediaSourceId: String?,
	position: Duration,
	fraction: Float,
	modifier: Modifier = Modifier,
) {
	val api = koinInject<ApiClient>()
	val info = remember(item, mediaSourceId) { item?.trickplayInfo(mediaSourceId) } ?: return
	val sourceUuid = mediaSourceId?.toUUIDOrNull() ?: return
	val tile = trickplayTile(position, info)
	val context = LocalContext.current
	val palette = LocalEphyraPalette.current
	val height = PreviewWidth * info.height / info.width.coerceAtLeast(1)

	val itemId = requireNotNull(item).id
	val request = remember(itemId, sourceUuid, tile) {
		ImageRequest.Builder(context)
			.data(api.trickPlayApi.getTrickplayTileImageUrl(itemId = itemId, width = info.width, index = tile.index, mediaSourceId = sourceUuid))
			.size(Size.ORIGINAL)
			.maxBitmapSize(Size(Dimension.Undefined, Dimension.Undefined))
			.httpHeaders(
				NetworkHeaders.Builder()
					.set(
						"Authorization",
						AuthorizationHeaderBuilder.buildHeader(
							api.clientInfo.name,
							api.clientInfo.version,
							api.deviceInfo.id,
							api.deviceInfo.name,
							api.accessToken,
						),
					)
					.build()
			)
			.transformations(SubsetTransformation(tile.x, tile.y, tile.width, tile.height))
			.build()
	}

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = modifier.layout { measurable, constraints ->
			val placeable = measurable.measure(Constraints())
			val gap = PreviewGap.roundToPx()
			val x = (constraints.maxWidth * fraction - placeable.width / 2f).toInt()
				.coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
			layout(constraints.maxWidth, 0) { placeable.place(x, -placeable.height - gap) }
		},
	) {
		AsyncImage(
			model = request,
			contentDescription = null,
			contentScale = ContentScale.Crop,
			modifier = Modifier
				.size(PreviewWidth, height)
				.clip(PreviewShape)
				.background(palette.surface2, PreviewShape)
				.border(2.dp, palette.ink, PreviewShape),
		)
		Text(
			text = formatPlaybackTime(position.inWholeMilliseconds),
			style = LocalTextStyle.current.merge(color = palette.ink, fontSize = 14.sp, fontFeatureSettings = "tnum"),
			modifier = Modifier.padding(top = 6.dp),
		)
	}
}
