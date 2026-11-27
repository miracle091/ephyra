package org.jellyfin.androidtv.ui.player.video

import android.view.KeyEvent

/** Velella: what a remote or gamepad key does in the video player, beyond moving the focus. */
enum class VideoPlayerKeyAction {
	PLAY,
	PAUSE,
	PLAY_PAUSE,
	FAST_FORWARD,
	REWIND,
	STOP,
	SUBTITLES_MENU,
	AUDIO_MENU,
	QUICK_SEEK_FORWARD,
	QUICK_SEEK_REWIND,
}

/**
 * Velella: the legacy player's keys (CustomPlaybackOverlayFragment): the media keys, R1/R2 and L1/L2 for fast forward and rewind,
 * Stop to close the player, Captions and Audio track to open those menus, and centre to play or pause while the controls are
 * hidden ([controlsVisible] false; with the controls on screen it presses the focused button). Left and right seek quickly while
 * the controls are hidden (QuickSeek.kt), which the legacy player did not do. Null for the other keys.
 */
fun videoPlayerKeyAction(keyCode: Int, controlsVisible: Boolean): VideoPlayerKeyAction? = when (keyCode) {
	KeyEvent.KEYCODE_MEDIA_PLAY -> VideoPlayerKeyAction.PLAY
	KeyEvent.KEYCODE_MEDIA_PAUSE -> VideoPlayerKeyAction.PAUSE
	KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> VideoPlayerKeyAction.PLAY_PAUSE
	KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_R2 -> VideoPlayerKeyAction.FAST_FORWARD
	KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_L2 -> VideoPlayerKeyAction.REWIND
	KeyEvent.KEYCODE_MEDIA_STOP -> VideoPlayerKeyAction.STOP
	KeyEvent.KEYCODE_CAPTIONS -> VideoPlayerKeyAction.SUBTITLES_MENU
	KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK -> VideoPlayerKeyAction.AUDIO_MENU
	// With the controls on screen, centre and the arrows move and press the focus
	else -> if (controlsVisible) null else hiddenControlsKeyAction(keyCode)
}

/** Velella: centre plays or pauses and left and right seek quickly while the controls are hidden. */
private fun hiddenControlsKeyAction(keyCode: Int): VideoPlayerKeyAction? = when (keyCode) {
	KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> VideoPlayerKeyAction.PLAY_PAUSE
	KeyEvent.KEYCODE_DPAD_RIGHT -> VideoPlayerKeyAction.QUICK_SEEK_FORWARD
	KeyEvent.KEYCODE_DPAD_LEFT -> VideoPlayerKeyAction.QUICK_SEEK_REWIND
	else -> null
}
