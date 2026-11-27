package org.jellyfin.androidtv.ui.player.video

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.livetv.TvManager
import org.jellyfin.androidtv.util.Utils
import org.koin.compose.koinInject
import java.util.UUID

private val LIVE_ICON_SIZE = 24.dp

/**
 * Velella: the live player's Guide, Record (when the user may manage recordings and the channel has a programme on air) and
 * Channels buttons, after the legacy player's (CustomPlaybackTransportControlGlue.updatePills), as icons only.
 */
@Composable
fun LiveTvPills(
	live: LiveChannelInfo,
	actions: LiveTvActions,
	userRepository: UserRepository = koinInject(),
) {
	val user by userRepository.currentUser.collectAsState()
	val program = live.program

	LivePill(R.drawable.ic_guide, stringResource(R.string.velella_osd_guide), actions.onGuide)
	if (program != null && Utils.canManageRecordings(user)) {
		val recording = program.isSetToRecord
		LivePill(
			icon = if (recording) R.drawable.ic_record_red else R.drawable.ic_record,
			label = stringResource(if (recording) R.string.velella_osd_recording else R.string.lbl_record),
			onClick = actions.onRecord,
			// The set state keeps the design system's danger colour (ic_record_red), focused or not
			iconTint = if (recording) Color.Unspecified else null,
		)
	}
	LivePill(R.drawable.ic_channel_bar, stringResource(R.string.velella_osd_channels), actions.onChannels)
}

/** A round icon button like the player's others (the user asked for icons only); the [label] is read out by TalkBack. */
@Composable
private fun LivePill(
	@DrawableRes icon: Int,
	label: String,
	onClick: () -> Unit,
	iconTint: Color? = null,
) = IconButton(onClick = onClick) {
	Icon(
		imageVector = ImageVector.vectorResource(icon),
		contentDescription = label,
		tint = iconTint ?: LocalTextStyle.current.color,
		modifier = Modifier.size(LIVE_ICON_SIZE),
	)
}

/** Velella: the legacy player's previous channel button (PreviousLiveTvChannelAction), when a channel was watched before this one. */
@Composable
fun PreviousChannelButton(currentChannel: UUID, onSwitch: (UUID) -> Unit) {
	val previous = remember(currentChannel) { TvManager.getPrevLiveTvChannel()?.takeIf { it != currentChannel } } ?: return
	IconButton(onClick = { onSwitch(previous) }) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_previous_episode),
			contentDescription = stringResource(R.string.lbl_prev_item),
		)
	}
}
