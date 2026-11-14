package org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.composable

import android.graphics.Typeface
import android.util.TypedValue
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.TypefaceCompat
import androidx.core.graphics.alpha
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.design.Tokens

/** Vertical position of the preview cue as a fraction of the view height: the middle. */
private const val CUE_LINE_FRACTION = 0.5f

/** The user's subtitle style settings that [SubtitleStylePreview] draws. */
@Immutable
data class SubtitleStyle(
	val backgroundColor: Long,
	val textWeight: Int,
	val textColor: Long,
	val strokeColor: Long,
	val textSize: Float,
) {
	companion object {
		fun from(userPreferences: UserPreferences) = SubtitleStyle(
			backgroundColor = userPreferences[UserPreferences.subtitlesBackgroundColor],
			textWeight = userPreferences[UserPreferences.subtitlesTextWeight],
			textColor = userPreferences[UserPreferences.subtitlesTextColor],
			strokeColor = userPreferences[UserPreferences.subtitleTextStrokeColor],
			textSize = userPreferences[UserPreferences.subtitlesTextSize],
		)
	}
}

@OptIn(UnstableApi::class)
@Composable
fun SubtitleStylePreview(style: SubtitleStyle) {
	val context = LocalContext.current
	SubtitleStylePreview(
		text = stringResource(R.string.subtitle_preview_text),
		textSize = style.textSize,
		captionStyle = CaptionStyleCompat(
			style.textColor.toInt(),
			style.backgroundColor.toInt(),
			0,
			if (style.strokeColor.toInt().alpha > 0) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE,
			style.strokeColor.toInt(),
			TypefaceCompat.create(context, Typeface.DEFAULT, style.textWeight, false),
		),
		modifier = Modifier
			.background(Tokens.Color.colorBluegrey800, JellyfinTheme.shapes.large)
			.fillMaxWidth()
			.height(style.textSize * 3.dp)
			.clip(JellyfinTheme.shapes.large)
	)
	Spacer(Modifier.height(Tokens.Space.spaceSm))
}

@OptIn(UnstableApi::class)
@Composable
private fun SubtitleStylePreview(
	modifier: Modifier = Modifier,
	text: String = stringResource(R.string.app_name),
	textSize: Float = 12f,
	captionStyle: CaptionStyleCompat = CaptionStyleCompat.DEFAULT,
) = AndroidView(
	modifier = modifier,
	factory = ::SubtitleView,
	update = { view ->
		view.setFixedTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)

		view.setStyle(captionStyle)

		val cue = Cue.Builder().apply {
			setText(text)
			setLine(CUE_LINE_FRACTION, Cue.LINE_TYPE_FRACTION)
			setLineAnchor(Cue.ANCHOR_TYPE_MIDDLE)
		}.build()

		view.setCues(listOf(cue))
	}
)
