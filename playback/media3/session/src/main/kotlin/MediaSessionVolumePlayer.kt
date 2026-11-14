package org.jellyfin.playback.media3.session

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.jellyfin.playback.core.PlayerVolumeState

private const val PERCENT = 100

/**
 * Base of [MediaSessionPlayer] that forwards the device volume commands of the media session to the player volume.
 */
@OptIn(UnstableApi::class)
internal abstract class MediaSessionVolumePlayer(
	looper: Looper,
	private val volume: PlayerVolumeState,
) : SimpleBasePlayer(looper) {
	protected fun currentDeviceVolumePercent(): Int = (volume.volume * PERCENT).toInt()

	override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> {
		volume.increaseVolume()
		return Futures.immediateVoidFuture()
	}

	override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> {
		volume.decreaseVolume()
		return Futures.immediateVoidFuture()
	}

	override fun handleSetDeviceVolume(deviceVolume: Int, flags: Int): ListenableFuture<*> {
		volume.setVolume(deviceVolume / PERCENT.toFloat())
		return Futures.immediateVoidFuture()
	}

	override fun handleSetDeviceMuted(muted: Boolean, flags: Int): ListenableFuture<*> {
		if (muted) volume.mute()
		else volume.unmute()
		return Futures.immediateVoidFuture()
	}
}
