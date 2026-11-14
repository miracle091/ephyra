package org.jellyfin.androidtv.ui.settings.screen.customization.subtitle

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.ColorSwatch
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.base.form.RangeControlState
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListControl
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.composable.SubtitleStyle
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.composable.SubtitleStylePreview
import org.jellyfin.design.Tokens
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private const val MIN_FONT_WEIGHT = 100
private const val MAX_FONT_WEIGHT = 900
private const val FONT_WEIGHT_STEP = 100
private const val PERCENT_FACTOR = 100f

@Composable
fun SettingsSubtitlesScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	var subtitlesBackgroundColor by rememberPreference(userPreferences, UserPreferences.subtitlesBackgroundColor)
	var subtitlesTextWeight by rememberPreference(userPreferences, UserPreferences.subtitlesTextWeight)
	var subtitlesTextColor by rememberPreference(userPreferences, UserPreferences.subtitlesTextColor)
	var subtitleTextStrokeColor by rememberPreference(userPreferences, UserPreferences.subtitleTextStrokeColor)
	var subtitlesTextSize by rememberPreference(userPreferences, UserPreferences.subtitlesTextSize)
	var subtitlesOffsetPosition by rememberPreference(userPreferences, UserPreferences.subtitlesOffsetPosition)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_subtitles)) },
			)
		}

		item {
			SubtitleStylePreview(
				SubtitleStyle(
					backgroundColor = subtitlesBackgroundColor,
					textWeight = subtitlesTextWeight,
					textColor = subtitlesTextColor,
					strokeColor = subtitleTextStrokeColor,
					textSize = subtitlesTextSize,
				)
			)
		}

		item { SubtitleSizeControl(value = subtitlesTextSize, onValueChange = { subtitlesTextSize = it }) }

		item { SubtitleWeightControl(value = subtitlesTextWeight, onValueChange = { subtitlesTextWeight = it }) }

		item {
			SubtitleColorButton(
				heading = R.string.lbl_subtitle_text_color,
				color = subtitlesTextColor,
				onClick = { router.push(Routes.CUSTOMIZATION_SUBTITLES_TEXT_COLOR) },
				key = Routes.CUSTOMIZATION_SUBTITLES_TEXT_COLOR,
			)
		}

		item {
			SubtitleColorButton(
				heading = R.string.lbl_subtitle_background_color,
				color = subtitlesBackgroundColor,
				onClick = { router.push(Routes.CUSTOMIZATION_SUBTITLES_BACKGROUND_COLOR) },
				key = Routes.CUSTOMIZATION_SUBTITLES_BACKGROUND_COLOR,
			)
		}

		item {
			SubtitleColorButton(
				heading = R.string.lbl_subtitle_text_stroke_color,
				color = subtitleTextStrokeColor,
				onClick = { router.push(Routes.CUSTOMIZATION_SUBTITLES_EDGE_COLOR) },
				key = Routes.CUSTOMIZATION_SUBTITLES_EDGE_COLOR,
			)
		}

		item { SubtitlePositionControl(value = subtitlesOffsetPosition, onValueChange = { subtitlesOffsetPosition = it }) }
	}
}

@Composable
private fun SubtitleSizeControl(value: Float, onValueChange: (Float) -> Unit) {
	val interactionSource = remember { MutableInteractionSource() }

	ListControl(
		headingContent = { Text(stringResource(R.string.pref_subtitles_size)) },
		interactionSource = interactionSource,
		modifier = Modifier.focusKey("subtitles_size"),
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
						state = RangeControlState(
							value = value,
							min = 8f,
							max = 48f,
							step = 1f,
							markers = listOf(UserPreferences.subtitlesTextSize.defaultValue),
						),
						onValueChange = onValueChange
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 32.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						Text(value.roundToInt().toString())
					}
				}
			},
		)
	)
}

@Composable
private fun SubtitleWeightControl(value: Int, onValueChange: (Int) -> Unit) {
	val interactionSource = remember { MutableInteractionSource() }

	ListControl(
		headingContent = { Text(stringResource(R.string.pref_subtitles_weight)) },
		slots = ListItemSlots(
			captionContent = {
				val normalizedValue = value
					.coerceIn(MIN_FONT_WEIGHT, MAX_FONT_WEIGHT)
					.floorDiv(FONT_WEIGHT_STEP)
					.times(FONT_WEIGHT_STEP)

				val name = when (normalizedValue) {
					100 -> stringResource(R.string.font_weight_100)
					200 -> stringResource(R.string.font_weight_200)
					300 -> stringResource(R.string.font_weight_300)
					400 -> stringResource(R.string.font_weight_400)
					500 -> stringResource(R.string.font_weight_500)
					600 -> stringResource(R.string.font_weight_600)
					700 -> stringResource(R.string.font_weight_700)
					800 -> stringResource(R.string.font_weight_800)
					900 -> stringResource(R.string.font_weight_900)
					else -> normalizedValue.toString()
				}
				Text(name)
			},
			footerContent = {
				RangeControl(
					modifier = Modifier
						.fillMaxWidth()
						.height(4.dp),
					interactionSource = interactionSource,
					state = RangeControlState(
						value = value.toFloat(),
						min = 100f,
						max = 900f,
						step = 100f,
						markers = listOf(UserPreferences.subtitlesTextWeight.defaultValue.toFloat()),
					),
					onValueChange = { onValueChange(it.toInt()) }
				)
			},
		),
		interactionSource = interactionSource,
		modifier = Modifier.focusKey("subtitles_weight")
	)
}

@Composable
private fun SubtitleColorButton(
	@StringRes heading: Int,
	color: Long,
	onClick: () -> Unit,
	key: String,
) {
	ListButton(
		headingContent = { Text(stringResource(heading)) },
		slots = ListItemSlots(
			trailingContent = { ColorSwatch(color = Color(color.toInt())) },
		),
		onClick = onClick,
		modifier = Modifier.focusKey(key)
	)
}

@Composable
private fun SubtitlePositionControl(value: Float, onValueChange: (Float) -> Unit) {
	val interactionSource = remember { MutableInteractionSource() }

	ListControl(
		headingContent = { Text(stringResource(R.string.pref_subtitles_position)) },
		interactionSource = interactionSource,
		modifier = Modifier.focusKey("subtitles_position"),
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
						state = RangeControlState(
							value = value,
							min = 0f,
							max = 0.8f,
							step = 0.01f,
							markers = listOf(UserPreferences.subtitlesOffsetPosition.defaultValue),
						),
						onValueChange = onValueChange
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 32.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						Text("${(value * PERCENT_FACTOR).roundToInt()}%")
					}
				}
			},
		)
	)
}
