package org.jellyfin.playback.media3.exoplayer.shield

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * Hardware video decoders first. For the heavy formats, software-only decoders are dropped when another decoder exists, so a hardware
 * decoder that fails to start surfaces as a playback error instead of a software decode the CPU can't keep up with. Decoder fallback
 * still moves between the remaining decoders. "Software-only" is the same flag the device profile uses (MediaCodecQuery).
 */
@OptIn(UnstableApi::class)
class ShieldMediaCodecSelector(
	private val delegate: MediaCodecSelector = MediaCodecSelector.DEFAULT,
) : MediaCodecSelector {
	companion object {
		/** Formats whose software decoders are skipped when a hardware one exists. The device profile follows the same list. */
		val HARDWARE_ONLY_MIME_TYPES = setOf(
			MimeTypes.VIDEO_H264,
			MimeTypes.VIDEO_H265,
			MimeTypes.VIDEO_DOLBY_VISION,
			MimeTypes.VIDEO_VP9,
		)
	}

	override fun getDecoderInfos(
		mimeType: String,
		requiresSecureDecoder: Boolean,
		requiresTunnelingDecoder: Boolean,
	): List<MediaCodecInfo> {
		val all = delegate.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
		if (!MimeTypes.isVideo(mimeType)) return all

		val (software, hardware) = all.partition { it.softwareOnly }
		return when {
			// No hardware decoder (AV1 on the Tegra X1+, emulators): software is the only option
			hardware.isEmpty() -> all
			mimeType in HARDWARE_ONLY_MIME_TYPES -> hardware
			else -> hardware + software
		}
	}
}
