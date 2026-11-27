package org.jellyfin.androidtv.ui.player.video

import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.preference.constant.StillWatchingBehavior
import org.jellyfin.sdk.model.api.BaseItemKind

/** Velella: what the new player asks when an entry ends and another one follows. */
enum class EndOfEntryPrompt {
	/** Nothing: the next entry plays at once. */
	NONE,

	/** "Next up", with a countdown that plays the next entry. */
	NEXT_UP,

	/** "Are you still watching?", with a countdown that closes the player. */
	STILL_WATCHING,
}

/**
 * Velella: the legacy player's choice at the end of an item (`PlaybackController.itemComplete`): still watching once it is due
 * ([stillWatchingDue], after the episodes or watch time of [stillWatching]), else next up when it is on; trailers never prompt.
 * Unlike the legacy player, a still watching that is not due yet with next up off moves on instead of closing the player.
 */
fun endOfEntryPrompt(
	endedType: BaseItemKind?,
	nextUp: NextUpBehavior,
	stillWatching: StillWatchingBehavior,
	stillWatchingDue: Boolean,
): EndOfEntryPrompt = when {
	endedType == BaseItemKind.TRAILER -> EndOfEntryPrompt.NONE
	stillWatching.enabled && stillWatchingDue -> EndOfEntryPrompt.STILL_WATCHING
	nextUp != NextUpBehavior.DISABLED -> EndOfEntryPrompt.NEXT_UP
	else -> EndOfEntryPrompt.NONE
}

/** Velella: what the new player shows over the picture. */
enum class PlayerOverlay {
	/** The playback controls. */
	CONTROLS,

	/** The [EndOfEntryPrompt] asked while the queue holds. */
	END_OF_ENTRY,

	/** Nothing. */
	NONE,
}

/**
 * Velella: the overlay for [prompt], asked at the end of an entry until the next one starts, while the queue is [holding] or not.
 * When the hold ends without a new entry (the player is closing), the controls must not come back: their focus request on the
 * way out leaves the screen below without focus, and its next D-pad key crashes Compose's focus search.
 */
fun playerOverlay(prompt: EndOfEntryPrompt, holding: Boolean): PlayerOverlay = when {
	prompt == EndOfEntryPrompt.NONE -> PlayerOverlay.CONTROLS
	holding -> PlayerOverlay.END_OF_ENTRY
	else -> PlayerOverlay.NONE
}
