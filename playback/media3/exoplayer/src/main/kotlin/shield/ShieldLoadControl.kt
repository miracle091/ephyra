package org.jellyfin.playback.media3.exoplayer.shield

import android.app.ActivityManager
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl

/**
 * ExoPlayer buffers media on the Java heap. Replace Media3's fixed ~137 MiB default with a cap of a quarter of the large heap (the
 * manifest sets android:largeHeap), so a 4K remux at 80-120 Mbps stays within the heap whatever buffer length the user picked.
 */
@OptIn(UnstableApi::class)
object ShieldLoadControl {
	private const val MB = 1024 * 1024
	private const val DEFAULT_LARGE_MEMORY_CLASS_MB = 256
	private const val HEAP_SHARE_DIVISOR = 4
	private const val MIN_TARGET_BUFFER_BYTES = 48 * MB
	private const val MAX_TARGET_BUFFER_BYTES = 192 * MB

	fun targetBufferBytes(largeMemoryClassMb: Int): Int =
		(largeMemoryClassMb.toLong() * MB / HEAP_SHARE_DIVISOR)
			.coerceIn(MIN_TARGET_BUFFER_BYTES.toLong(), MAX_TARGET_BUFFER_BYTES.toLong())
			.toInt()

	@JvmStatic
	@JvmOverloads
	fun build(
		context: Context,
		minBufferMs: Int = DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
		maxBufferMs: Int = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
		bufferForPlaybackMs: Int = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
		bufferForPlaybackAfterRebufferMs: Int = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
	): DefaultLoadControl {
		val largeMemoryClassMb = context.getSystemService(ActivityManager::class.java)?.largeMemoryClass ?: DEFAULT_LARGE_MEMORY_CLASS_MB

		return DefaultLoadControl.Builder()
			.setBufferDurationsMs(minBufferMs, maxBufferMs, bufferForPlaybackMs, bufferForPlaybackAfterRebufferMs)
			.setTargetBufferBytes(targetBufferBytes(largeMemoryClassMb))
			.build()
	}
}
