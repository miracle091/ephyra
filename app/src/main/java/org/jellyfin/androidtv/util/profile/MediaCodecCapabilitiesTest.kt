package org.jellyfin.androidtv.util.profile

import android.media.MediaCodecList
import android.util.Size
import org.jellyfin.androidtv.util.profile.codec.Av1CodecCapabilities
import org.jellyfin.androidtv.util.profile.codec.AvcCodecCapabilities
import org.jellyfin.androidtv.util.profile.codec.HevcCodecCapabilities
import org.jellyfin.androidtv.util.profile.codec.MediaCodecQuery

class MediaCodecCapabilitiesTest(
	private val softwareCodecsEnabled: Boolean,
) {
	private val mediaCodecList by lazy { MediaCodecList(MediaCodecList.REGULAR_CODECS) }
	internal val codecQuery by lazy { MediaCodecQuery(mediaCodecList, softwareCodecsEnabled) }
	internal val avc by lazy { AvcCodecCapabilities(codecQuery) }
	private val hevc by lazy { HevcCodecCapabilities(codecQuery) }
	internal val av1 by lazy { Av1CodecCapabilities(codecQuery) }

	fun supportsAV1(): Boolean = av1.supportsAv1()

	// Velella: true only with an AV1 decoder that is not software-only (none on any Shield TV)
	fun supportsHardwareAV1(): Boolean = Av1CodecCapabilities(MediaCodecQuery(mediaCodecList, softwareCodecsEnabled = false)).supportsAv1()

	fun supportsAVC(): Boolean = avc.supportsAvc()

	fun getAVCMainLevel(): Int = avc.getMainLevel()

	fun supportsHevc(): Boolean = hevc.supportsHevc()

	fun supportsHevcMain10(): Boolean = hevc.supportsHevcMain10()

	fun supportsHevcDolbyVision(): Boolean = hevc.supportsHevcDolbyVision()

	fun supportsHevcDolbyVisionEL(): Boolean = hevc.supportsHevcDolbyVisionEL()

	fun supportsHevcHDR10(): Boolean = hevc.supportsHevcHDR10()

	fun supportsHevcHDR10Plus(): Boolean = hevc.supportsHevcHDR10Plus()

	fun getHevcMainLevel(): Int = hevc.getMainLevel()

	fun getHevcMain10Level(): Int = hevc.getMain10Level()

	fun getMaxResolution(mime: String): Size = codecQuery.getMaxResolution(mime)
}
