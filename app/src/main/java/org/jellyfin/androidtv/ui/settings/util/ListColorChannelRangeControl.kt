package org.jellyfin.androidtv.ui.settings.util

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.base.form.RangeControlState
import org.jellyfin.androidtv.ui.base.form.RangeControlDefaults
import org.jellyfin.androidtv.ui.base.list.ListControl
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.design.Tokens
import kotlin.math.roundToInt

private const val PERCENT_FACTOR = 100f

@Composable
fun ListColorChannelRangeControl(
	headingContent: @Composable () -> Unit,
	channel: Color,
	value: Color,
	onValueChange: (color: Color) -> Unit,
	modifier: Modifier = Modifier,
) {
	val interactionSource = remember { MutableInteractionSource() }

	val channelValue = value.channelValue(channel)
	val opaqueValue = value.copy(alpha = 1f)
	val backgroundBrush = Brush.horizontalGradient(
		listOf(
			opaqueValue.withChannel(channel, 0f),
			opaqueValue.withChannel(channel, 1f),
		)
	)

	ListControl(
		headingContent = headingContent,
		interactionSource = interactionSource,
		modifier = modifier,
		slots = ListItemSlots(
			footerContent = {
				Row(
					verticalAlignment = Alignment.CenterVertically,
				) {
					RangeControl(
						modifier = Modifier
							.height(4.dp)
							.weight(1f)
							.background(backgroundBrush, RoundedCornerShape(2.dp)),
						interactionSource = interactionSource,
						state = RangeControlState(value = channelValue),
						onValueChange = { onValueChange(value.withChannel(channel, it)) },
						colors = RangeControlDefaults.colors(
							backgroundColor = Color.Transparent,
							fillColor = Color.Transparent
						)
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 32.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						Text("${(channelValue * PERCENT_FACTOR).roundToInt()}%")
					}
				}
			},
		),
	)
}

/** The value of one channel of this colour; [channel] is [Color.Red], [Color.Green], [Color.Blue] or [Color.Transparent] for alpha. */
private fun Color.channelValue(channel: Color) = when (channel) {
	Color.Red -> red
	Color.Green -> green
	Color.Blue -> blue
	Color.Transparent -> alpha
	else -> error("Unknown color channel $channel")
}

/** This colour with one channel replaced, see [channelValue] for how the channel is identified. */
private fun Color.withChannel(channel: Color, newValue: Float) = Color(
	red = if (channel == Color.Red) newValue else red,
	green = if (channel == Color.Green) newValue else green,
	blue = if (channel == Color.Blue) newValue else blue,
	alpha = if (channel == Color.Transparent) newValue else alpha,
)
