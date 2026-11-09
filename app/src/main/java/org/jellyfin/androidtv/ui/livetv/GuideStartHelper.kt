package org.jellyfin.androidtv.ui.livetv

import java.util.UUID

/**
 * Ephyra: index of the first channel the guide loads. With [restoreLastChannel] a last channel beyond the first page is centred
 * in its page; otherwise, or when it is in the first page, the guide starts at the top.
 */
fun guideStartIndex(lastChannelIndex: Int, pageSize: Int, restoreLastChannel: Boolean): Int =
	if (restoreLastChannel && lastChannelIndex >= pageSize) lastChannelIndex - pageSize / 2 else 0

/** Ephyra: the channel the guide focuses on opening: the last tuned one, or none to focus the first row. */
fun guideFocusChannelId(lastChannelId: UUID?, restoreLastChannel: Boolean): UUID? = lastChannelId.takeIf { restoreLastChannel }
