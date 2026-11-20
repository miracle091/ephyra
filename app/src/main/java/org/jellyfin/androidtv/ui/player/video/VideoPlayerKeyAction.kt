package org.jellyfin.androidtv.ui.player.video

import android.view.KeyEvent

/** Ephyra: what a remote or gamepad key does in the video player, beyond moving the focus. */
enum class VideoPlayerKeyAction {
	PLAY,
	PAUSE,
	PLAY_PAUSE,
	FAST_FORWARD,
	REWIND,
	STOP,
	SUBTITLES_MENU,
	AUDIO_MENU,
}

/**
 * Ephyra: the legacy player's keys (CustomPlaybackOverlayFragment): the media keys, R1/R2 and L1/L2 for fast forward and rewind,
 * Stop to close the player, Captions and Audio track to open those menus, and centre to play or pause while the controls are
 * hidden ([controlsVisible] false; with the controls on screen it presses the focused button). Null for the other keys.
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
	KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> if (controlsVisible) null else VideoPlayerKeyAction.PLAY_PAUSE
	else -> null
}
