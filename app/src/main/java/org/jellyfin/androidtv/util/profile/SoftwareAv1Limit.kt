package org.jellyfin.androidtv.util.profile

/**
 * Resolution cap for AV1 when only a software decoder exists, as on every Shield TV (Tegra X1 and X1+):
 * their CPU cores keep up with 1080p at most, so the server transcodes anything larger. Hardware AV1 decoders keep their own limits.
 */
object SoftwareAv1Limit {
	const val MAX_WIDTH = 1920
	const val MAX_HEIGHT = 1080

	fun width(decoderMaxWidth: Int, softwareOnly: Boolean): Int = if (softwareOnly) minOf(decoderMaxWidth, MAX_WIDTH) else decoderMaxWidth

	fun height(decoderMaxHeight: Int, softwareOnly: Boolean): Int = if (softwareOnly) minOf(decoderMaxHeight, MAX_HEIGHT) else decoderMaxHeight
}
