package org.jellyfin.androidtv.ui.base.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.ProvideTextStyle

// Ephyra: a screen header with the home spotlight's type scale: overline 11 in the accent, title 28, caption 14
private val SectionOverline = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
private val SectionHeading = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
private val SectionCaption = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)

@Composable
fun ListSection(
	modifier: Modifier = Modifier,
	headingContent: @Composable () -> Unit,
	overlineContent: (@Composable () -> Unit)? = null,
	captionContent: (@Composable () -> Unit)? = null,
	leadingContent: (@Composable () -> Unit)? = null,
	trailingContent: (@Composable () -> Unit)? = null,
	footerContent: (@Composable () -> Unit)? = null,
) {
	val colors = JellyfinTheme.colorScheme
	ListItemContent(
		headingContent = headingContent,
		overlineContent = overlineContent?.let { content ->
			{ ProvideTextStyle(SectionOverline.copy(color = colors.badge), content) }
		},
		captionContent = captionContent?.let { content ->
			{ ProvideTextStyle(SectionCaption.copy(color = colors.listCaption), content) }
		},
		leadingContent = leadingContent,
		trailingContent = trailingContent,
		footerContent = footerContent,
		headingStyle = SectionHeading.copy(color = colors.listHeader),
		modifier = modifier,
	)
}
