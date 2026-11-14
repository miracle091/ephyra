package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.base.form.RangeControlState
import org.jellyfin.androidtv.ui.base.list.ListControl
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.design.Tokens
import java.text.DecimalFormat
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val MILLIS_PER_SECOND = 1000
private const val MILLIS_PER_SECOND_FLOAT = 1000f

@Composable
internal fun SkipForwardLengthControl(value: Int, onValueChange: (Int) -> Unit) {
	val interactionSource = remember { MutableInteractionSource() }

	ListControl(
		headingContent = { Text(stringResource(R.string.skip_forward_length)) },
		interactionSource = interactionSource,
		modifier = Modifier.focusKey("skip_forward_length"),
		slots = ListItemSlots(
			footerContent = {
				Row(
					verticalAlignment = Alignment.CenterVertically,
				) {
					RangeControl(
						modifier = Modifier
							.height(4.dp)
							.weight(1f),
						interactionSource = interactionSource,
						// 5 - 30 seconds with 5 second increment
						state = RangeControlState(
							value = value.toFloat(),
							min = 5_000f,
							max = 30_000f,
							step = 5_000f,
						),
						onValueChange = { onValueChange(it.roundToInt()) }
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 32.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						Text("${value / MILLIS_PER_SECOND}s")
					}
				}
			},
		)
	)
}

@Composable
internal fun VideoStartDelayControl(value: Long, onValueChange: (Long) -> Unit) {
	val interactionSource = remember { MutableInteractionSource() }

	ListControl(
		headingContent = { Text(stringResource(R.string.video_start_delay)) },
		interactionSource = interactionSource,
		modifier = Modifier.focusKey("video_start_delay"),
		slots = ListItemSlots(
			footerContent = {
				Row(
					verticalAlignment = Alignment.CenterVertically,
				) {
					RangeControl(
						modifier = Modifier
							.height(4.dp)
							.weight(1f),
						interactionSource = interactionSource,
						// 0 - 5 seconds with 0.25 second increment
						state = RangeControlState(
							value = value.toFloat(),
							min = 0_000f,
							max = 5_000f,
							step = 250f,
						),
						onValueChange = { onValueChange(it.roundToLong()) }
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 48.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						val formatter = remember { DecimalFormat("0.##") }
						Text("${formatter.format(value / MILLIS_PER_SECOND_FLOAT)}s")
					}
				}
			},
		)
	)
}
