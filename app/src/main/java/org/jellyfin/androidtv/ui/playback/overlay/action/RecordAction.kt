package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.view.View
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class RecordAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	companion object {
		const val INDEX_INACTIVE = 0
		const val INDEX_RECORDING = 1
	}

	/**
	 * Velella: the player's buttons tint their icon (ink, background colour when focused). The set state keeps the design
	 * system's danger colour of ic_record_red, so its drawable ignores the tint.
	 */
	private class UntintedDrawable(drawable: Drawable) : InsetDrawable(drawable, 0) {
		override fun setTintList(tint: ColorStateList?) = Unit
		override fun setTintMode(tintMode: PorterDuff.Mode?) = Unit
		override fun setTintBlendMode(blendMode: BlendMode?) = Unit
	}

	init {
		val recordInactive = ContextCompat.getDrawable(context, R.drawable.ic_record)
		val recordActive = ContextCompat.getDrawable(context, R.drawable.ic_record_red)?.let(::UntintedDrawable)

		setDrawables(arrayOf(recordInactive, recordActive))
	}

	@Override
	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.toggleRecording()
	}
}
