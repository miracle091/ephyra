package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.Seekbar
import org.jellyfin.androidtv.ui.base.SeekbarDefaults
import org.jellyfin.androidtv.ui.base.SeekbarPosition
import org.jellyfin.androidtv.ui.base.SeekbarStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.playback.LiveProgram
import org.jellyfin.androidtv.ui.playback.OsdHeader
import org.jellyfin.androidtv.util.getTimeFormatter
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val LIVE_SEEKBAR_HEIGHT = 3.dp

/** Velella: the live player's header line as in the legacy player (OsdHeader.bind): the danger "LIVE" badge, then the channel. */
@Composable
fun LiveChannelLine(info: LiveChannelInfo) {
	val palette = LocalVelellaPalette.current
	Row(verticalAlignment = Alignment.CenterVertically) {
		LiveBadge()
		Text(
			text = OsdHeader.overlineOf(info.channel, ""),
			overflow = TextOverflow.Ellipsis,
			maxLines = 1,
			style = LocalTextStyle.current.merge(color = palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold),
			modifier = Modifier.padding(start = 12.dp),
		)
	}
}

/** The design system's danger badge: 20dp high, 4dp radius, a white dot and bold 11sp caps (velella_osd_live_badge). */
@Composable
private fun LiveBadge() {
	val onDanger = colorResource(R.color.velella_osd_on_danger)
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier
			.height(20.dp)
			.background(colorResource(R.color.velella_osd_danger), RoundedCornerShape(4.dp))
			.padding(horizontal = 6.dp),
	) {
		Box(Modifier.size(6.dp).background(onDanger, CircleShape))
		Text(
			text = stringResource(R.string.lbl_live).uppercase(),
			style = LocalTextStyle.current.merge(
				color = onDanger,
				fontSize = 11.sp,
				fontWeight = FontWeight.Bold,
				letterSpacing = 0.06.em,
			),
			modifier = Modifier.padding(start = 4.dp),
		)
	}
}

/**
 * Velella: the live player's block above the buttons, as in the legacy player (lb_playback_transport_controls_row): the
 * programme's genres and episode title, its name, the bar of the programme's elapsed share (not seekable), its start and end
 * times, then the next programme and how the server delivers the stream.
 */
@Composable
fun LiveProgramBlock(info: LiveChannelInfo, playInfo: String?) {
	val palette = LocalVelellaPalette.current
	val channel = remember(info) { info.program?.let { info.channel.copy(currentProgram = it) } ?: info.channel }
	val overline = remember(channel) { OsdHeader.programmeOverlineOf(channel) }
	val start = info.program?.startDate
	val end = info.program?.endDate

	Column(modifier = Modifier.fillMaxWidth()) {
		if (overline.isNotBlank()) {
			Text(
				text = overline.uppercase(),
				overflow = TextOverflow.Ellipsis,
				maxLines = 1,
				style = LocalTextStyle.current.merge(
					color = palette.inkMuted,
					fontSize = 11.sp,
					fontWeight = FontWeight.Bold,
					letterSpacing = 0.08.em,
				),
			)
		}
		Text(
			text = OsdHeader.titleOf(channel).orEmpty(),
			overflow = TextOverflow.Ellipsis,
			maxLines = 1,
			style = LocalTextStyle.current.merge(color = palette.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold),
			modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
		)

		LiveProgramBar(start, end)

		val muted = LocalTextStyle.current.merge(color = palette.inkMuted, fontSize = 14.sp, fontFeatureSettings = "tnum")
		if (start != null && end != null) {
			val formatter = LocalContext.current.getTimeFormatter()
			Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
				Text(formatter.format(start), style = muted)
				Spacer(Modifier.weight(1f))
				Text(formatter.format(end), style = muted)
			}
		}

		val next = info.next
		val nextStart = next?.startDate
		if (next != null && nextStart != null) {
			Text(
				text = stringResource(
					R.string.velella_osd_next_program,
					next.name.orEmpty(),
					LocalContext.current.getTimeFormatter().format(nextStart),
				),
				style = muted.merge(fontWeight = FontWeight.Bold),
				modifier = Modifier.padding(top = 6.dp),
			)
		}
		if (playInfo != null) Text(playInfo, style = muted, modifier = Modifier.padding(top = 4.dp))
	}
}

/** The programme's elapsed share, updated every second (LiveProgram), drawn with the seek bar's look but never focusable. */
@Composable
private fun LiveProgramBar(start: LocalDateTime?, end: LocalDateTime?) {
	var now by remember { mutableStateOf(LocalDateTime.now()) }
	LaunchedEffect(Unit) {
		while (true) {
			delay(1.seconds)
			now = LocalDateTime.now()
		}
	}
	val duration = LiveProgram.durationMs(start, end)
	val position = if (duration > 0) {
		SeekbarPosition(progress = LiveProgram.positionMs(start, end, now).milliseconds, duration = duration.milliseconds)
	} else {
		SeekbarPosition()
	}

	Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(24.dp)) {
		Seekbar(
			position = position,
			enabled = false,
			style = SeekbarStyle(SeekbarDefaults.colors()),
			modifier = Modifier.fillMaxWidth().height(LIVE_SEEKBAR_HEIGHT),
		)
	}
}
