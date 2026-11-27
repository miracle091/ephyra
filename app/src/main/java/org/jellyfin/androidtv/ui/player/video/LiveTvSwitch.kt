package org.jellyfin.androidtv.ui.player.video

import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Velella: how the video player's live TV controls tune to another channel ([switchChannel], from the channel bar, the guide and
 * the previous channel button). The queue is rebuilt for the new channel and is empty for a moment: while [switching], that is no
 * end of the queue, so the player stays open.
 */
class LiveTvSwitch(
	val switching: StateFlow<Boolean>,
	val switchChannel: (UUID) -> Unit,
)

/** Velella: what the live player's buttons do: the Guide, Record and Channels pills and the previous channel button. */
class LiveTvActions(
	val onGuide: () -> Unit,
	val onRecord: () -> Unit,
	val onChannels: () -> Unit,
	val onSwitch: (UUID) -> Unit,
)
