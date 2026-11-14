package org.jellyfin.androidtv.util.profile

import android.content.Context
import android.media.MediaCodecInfo
import androidx.media3.common.MimeTypes
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.util.MarkdownBuilder
import org.jellyfin.androidtv.util.profile.codec.isPassthroughAudioAvailable

// Names are copied from MediaCodecInfo.CodecCapabilities.FEATURE_xxx constants as some constants are not available in older API versions
private val featureNames = setOf(
	"adaptive-playback",
	"detached-surface",
	"dynamic-color-aspects",
	"dynamic-timestamp",
	"encoding-statistics",
	"frame-parsing",
	"hdr-editing",
	"hlg-editing",
	"intra-refresh",
	"low-latency",
	"multiple-frames",
	"partial-frame",
	"qp-bounds",
	"region-of-interest",
	"secure-playback",
	"tunneled-playback",
)

internal fun MarkdownBuilder.appendCodecDecoders(codecs: List<MediaCodecInfo>) {
	for (codec in codecs) {
		appendCodecHeader(codec)

		for (type in codec.supportedTypes) {
			appendCodecType(type, codec.getCapabilitiesForType(type))
		}

		appendLine()
	}
}

private fun MarkdownBuilder.appendCodecType(type: String, capabilities: MediaCodecInfo.CodecCapabilities) {
	appendLine("  - **$type**")

	capabilities.audioCapabilities?.let { appendAudioCapabilities(it) }
	capabilities.videoCapabilities?.let { appendVideoCapabilities(it) }

	capabilities.colorFormats?.takeIf { it.isNotEmpty() }?.let { colorFormats ->
		appendLine("    - colorFormats: ${colorFormats.joinToString(", ")}")
	}

	capabilities.profileLevels?.takeIf { it.isNotEmpty() }?.let { profileLevels ->
		appendLine("    - profileLevels")
		for (profileLevel in profileLevels) {
			appendLine("      - ${profileLevel.profile}: ${profileLevel.level}")
		}
	}

	appendFeatures(capabilities)
}

private fun MarkdownBuilder.appendCodecHeader(codec: MediaCodecInfo) {
	if (AndroidVersion.isAtLeastQ) appendLine("- **${codec.canonicalName} (${codec.name})**")
	else appendLine("- **${codec.name}**")

	if (AndroidVersion.isAtLeastQ) appendLine("  - isVendor: ${codec.isVendor}")
	if (AndroidVersion.isAtLeastQ) appendLine("  - isHardwareAccelerated: ${codec.isHardwareAccelerated}")
	if (AndroidVersion.isAtLeastQ) appendLine("  - isSoftwareOnly: ${codec.isSoftwareOnly}")
	if (AndroidVersion.isAtLeastQ) appendLine("  - isAlias: ${codec.isAlias}")
}

private fun MarkdownBuilder.appendAudioCapabilities(audio: MediaCodecInfo.AudioCapabilities) {
	if (AndroidVersion.isAtLeastS) appendLine("    - minInputChannelCount: ${audio.minInputChannelCount}")
	appendLine("    - maxInputChannelCount: ${audio.maxInputChannelCount}")
	if (AndroidVersion.isAtLeastS) audio.inputChannelCountRanges.takeIf { it.isNotEmpty() }?.let {
		appendLine("    - inputChannelCountRanges: ${it.joinToString(", ") { it.prettyFormat() }}")
	}
	audio.bitrateRange?.let { appendLine("    - bitrateRange: ${it.prettyFormat()}") }
	// Note: Fire OS has a bug in the getter for supportedSampleRates that throws NullPointerException
	runCatching { audio.supportedSampleRates }.getOrNull()?.takeIf { it.isNotEmpty() }?.let {
		appendLine("    - supportedSampleRates: ${it.joinToString(", ")}")
	}
	audio.supportedSampleRateRanges?.takeIf { it.isNotEmpty() }?.let {
		appendLine("    - supportedSampleRateRanges: ${it.joinToString(", ") { it.prettyFormat() }}")
	}
}

private fun MarkdownBuilder.appendVideoCapabilities(video: MediaCodecInfo.VideoCapabilities) {
	video.bitrateRange?.let { appendLine("    - bitrateRange: ${it.prettyFormat()}") }
	video.supportedFrameRates?.let { appendLine("    - supportedFrameRates: ${it.prettyFormat()}") }
	appendLine("    - widthAlignment: ${video.widthAlignment}")
	appendLine("    - heightAlignment: ${video.heightAlignment}")
	video.supportedWidths?.let {
		appendLine("    - supportedWidths: ${it.prettyFormat()}")
	}
	video.supportedHeights?.let {
		appendLine("    - supportedHeights: ${it.prettyFormat()}")
	}
	if (AndroidVersion.isAtLeastQ) video.supportedPerformancePoints?.takeIf { it.isNotEmpty() }?.let {
		appendLine("    - supportedPerformancePoints: ${it.joinToString(", ")}")
	}
}

private fun MarkdownBuilder.appendFeatures(capabilities: MediaCodecInfo.CodecCapabilities) {
	// Only show features section if there is at least 1
	featureNames.mapNotNull { name ->
		when {
			capabilities.isFeatureRequired(name) -> "$name (required)"
			capabilities.isFeatureSupported(name) -> name
			else -> null
		}
	}.takeIf { it.isNotEmpty() }?.let { features ->
		appendLine("    - features")
		for (feature in features) {
			appendLine("      - $feature")
		}
	}
}

internal fun MarkdownBuilder.appendPassthroughCapabilities(context: Context) {
	appendLine("***AC3 (2.0)***: ${isPassthroughAudioAvailable(context, MimeTypes.AUDIO_AC3)}")
	appendLine("***EAC3 (2.0)***: ${isPassthroughAudioAvailable(context, MimeTypes.AUDIO_E_AC3)}")
	appendLine("***DTS (2.0)***: ${isPassthroughAudioAvailable(context, MimeTypes.AUDIO_DTS)}")
	appendLine("***TrueHD (2.0)***: ${isPassthroughAudioAvailable(context, MimeTypes.AUDIO_TRUEHD)}")
	appendLine()
}

internal fun MarkdownBuilder.appendKnownMediaTypes(codecs: List<MediaCodecInfo>) {
	codecs
		.flatMap { codec -> codec.supportedTypes.asIterable() }
		.distinct()
		.sorted()
		.forEach { type -> appendLine("- $type") }
}

internal fun MarkdownBuilder.appendCodecHdrSupport(userPreferences: UserPreferences) {
	val mediaTest = MediaCodecCapabilitiesTest(userPreferences[UserPreferences.softwareCodecsEnabled])

	val codecHDRSupport = buildMap<String, Map<HdrFormats, Boolean>> {
		if (mediaTest.supportsAV1()) {
			put(
				Codec.Video.AV1, mapOf(
					HdrFormats.DOLBY_VISION to mediaTest.supportsAV1DolbyVision(),
					HdrFormats.HDR10 to mediaTest.supportsAV1HDR10(),
					HdrFormats.HDR10_PLUS to mediaTest.supportsAV1HDR10Plus()
				)
			)
		}
		if (mediaTest.supportsHevc()) {
			put(
				Codec.Video.HEVC, mapOf(
					HdrFormats.DOLBY_VISION to mediaTest.supportsHevcDolbyVision(),
					HdrFormats.DOLBY_VISION_EL to mediaTest.supportsHevcDolbyVisionEL(),
					HdrFormats.HDR10 to mediaTest.supportsHevcHDR10(),
					HdrFormats.HDR10_PLUS to mediaTest.supportsHevcHDR10Plus()
				)
			)
		}
	}

	for ((codec, formats) in codecHDRSupport) {
		appendLine("**${codec.uppercase()}**")
		for (format in formats) {
			appendLine("  - ${format.key.label}: ${format.value}")
		}
		appendLine()
	}
}
