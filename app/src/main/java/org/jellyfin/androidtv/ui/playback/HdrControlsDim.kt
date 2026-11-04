package org.jellyfin.androidtv.ui.playback

import android.view.View
import org.jellyfin.androidtv.data.compat.StreamInfo
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.VideoRange

/**
 * Ephyra: dims the player's controls while an HDR or Dolby Vision video plays, so white text and icons don't glare on an
 * HDR display.
 */
object HdrControlsDim {
	/** Opacity of the controls during HDR playback. */
	const val HDR_ALPHA = 0.6f

	/**
	 * True when the stream reaches the TV as HDR: the source video is HDR and the server sends it as it is (direct play or
	 * remux). A transcode is taken as SDR, as the server tone-maps HDR when it converts.
	 */
	fun isHdr(streamInfo: StreamInfo?): Boolean {
		if (streamInfo == null || streamInfo.playMethod == PlayMethod.TRANSCODE) return false
		val video = streamInfo.mediaSource?.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }
		return video?.videoRange == VideoRange.HDR
	}

	/** Sets the opacity of [views] for the stream in [streamInfo]. */
	@JvmStatic
	fun apply(streamInfo: StreamInfo?, vararg views: View) {
		val alpha = if (isHdr(streamInfo)) HDR_ALPHA else 1f
		for (view in views) view.alpha = alpha
	}
}
