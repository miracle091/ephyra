package org.jellyfin.androidtv.ui.playback

/**
 * Velella: dims the player's controls while an HDR or Dolby Vision video plays, so white text and icons don't glare on an
 * HDR display (the check is isHdrStream in VideoPlayerInfo.kt).
 */
object HdrControlsDim {
	/** Opacity of the controls during HDR playback. */
	const val HDR_ALPHA = 0.6f
}
