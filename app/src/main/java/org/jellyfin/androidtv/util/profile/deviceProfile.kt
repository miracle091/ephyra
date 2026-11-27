package org.jellyfin.androidtv.util.profile

import android.content.Context
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.androidtv.preference.constant.BitstreamAudioFormat
import org.jellyfin.androidtv.preference.constant.BitstreamAudioMode
import org.jellyfin.androidtv.preference.constant.HdrFormat
import org.jellyfin.androidtv.preference.constant.HdrOverrideMode
import org.jellyfin.androidtv.util.profile.codec.hasPlatformAudioDecoder
import org.jellyfin.androidtv.util.profile.codec.isPassthroughAudioAvailable
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.VideoRangeType
import org.jellyfin.sdk.model.deviceprofile.DeviceProfileBuilder
import org.jellyfin.sdk.model.deviceprofile.buildDeviceProfile
import kotlin.math.roundToInt

private const val MIN_VALID_BITRATE_MBPS = 0.01f
private const val BITS_PER_MEGABIT = 1_000_000

private val downmixSupportedAudioCodecs = arrayOf(
	Codec.Audio.AAC,
	Codec.Audio.MP2,
	Codec.Audio.MP3,
)

private val supportedAudioCodecs = arrayOf(
	Codec.Audio.AAC,
	Codec.Audio.AAC_LATM,
	Codec.Audio.AC3,
	Codec.Audio.ALAC,
	Codec.Audio.DCA,
	Codec.Audio.DTS,
	Codec.Audio.EAC3,
	Codec.Audio.FLAC,
	// Velella: no MLP, neither Media3's extractors nor the FFmpeg extension play it: the server converts it
	Codec.Audio.MP2,
	Codec.Audio.MP3,
	Codec.Audio.OPUS,
	Codec.Audio.PCM_ALAW,
	Codec.Audio.PCM_MULAW,
	Codec.Audio.PCM_S16LE,
	Codec.Audio.PCM_S20LE,
	Codec.Audio.PCM_S24LE,
	Codec.Audio.TRUEHD,
	Codec.Audio.VORBIS,
)

// Velella: the server encodes audio to the first allowed codec: a 5.1 Dolby bitstream beats AAC (the bitstream settings filter it)
private val hlsMpegTsAudioCodecs = arrayOf(
	Codec.Audio.AC3,
	Codec.Audio.EAC3,
	Codec.Audio.AAC,
	Codec.Audio.MP3
)

private val hlsFmp4AudioCodecs = arrayOf(
	Codec.Audio.AC3,
	Codec.Audio.EAC3,
	Codec.Audio.AAC,
	Codec.Audio.MP3,
	Codec.Audio.ALAC,
	Codec.Audio.FLAC,
	Codec.Audio.OPUS,
	Codec.Audio.DTS,
)

// Velella: direct play containers whose video rotation Media3 does not apply reliably (everything except the MP4 family and HLS)
internal val rotationUnawareContainers = listOf(
	Codec.Container.ASF,
	Codec.Container.MKV,
	Codec.Container.OGM,
	Codec.Container.OGV,
	Codec.Container.TS,
	Codec.Container.VOB,
	Codec.Container.WEBM,
	Codec.Container.WMV,
	Codec.Container.XVID,
)

private fun UserPreferences.getMaxBitrate(): Int {
	var maxBitrate = this[UserPreferences.maxBitrate].toFloatOrNull()

	// The value "0" was used in an older release, make sure we prevent that from being used to avoid video not playing
	if (maxBitrate == null || maxBitrate < MIN_VALID_BITRATE_MBPS) maxBitrate = UserPreferences.maxBitrate.defaultValue.toFloat()

	// Convert megabit to bit
	return (maxBitrate * BITS_PER_MEGABIT).roundToInt()
}

private fun UserPreferences.getHdrRangeTypesFor(mode: HdrOverrideMode): Set<VideoRangeType> =
	HdrFormat.entries
		.filter { this[it.preference] == mode }
		.flatMapTo(mutableSetOf()) { it.videoRangeTypes }

private fun UserPreferences.isBitstreamAudioEnabled(context: Context, format: BitstreamAudioFormat): Boolean =
	when (this[format.preference]) {
		// Velella: a sink that refuses the bitstream is not the end when a platform decoder can play the format
		BitstreamAudioMode.AUTO -> isPassthroughAudioAvailable(context, format.mimeType) || hasPlatformAudioDecoder(format.mimeType)
		BitstreamAudioMode.ENABLE -> true
		BitstreamAudioMode.DISABLE -> false
	}

fun createDeviceProfile(
	context: Context,
	userPreferences: UserPreferences,
): DeviceProfile = createDeviceProfile(
	mediaTest = MediaCodecCapabilitiesTest(userPreferences[UserPreferences.softwareCodecsEnabled]),
	options = DeviceProfileOptions(
		maxBitrate = userPreferences.getMaxBitrate(),
		audio = AudioProfileOptions(
			isAC3PrefEnabled = userPreferences.isBitstreamAudioEnabled(context, BitstreamAudioFormat.AC3),
			isEAC3PrefEnabled = userPreferences.isBitstreamAudioEnabled(context, BitstreamAudioFormat.EAC3),
			isDTSPrefEnabled = userPreferences.isBitstreamAudioEnabled(context, BitstreamAudioFormat.DTS),
			isTrueHDPrefEnabled = userPreferences.isBitstreamAudioEnabled(context, BitstreamAudioFormat.TRUEHD),
			downMixAudio = userPreferences[UserPreferences.audioBehaviour] == AudioBehavior.DOWNMIX_TO_STEREO,
		),
		subtitles = SubtitleProfileOptions(
			assDirectPlay = userPreferences[UserPreferences.assDirectPlay],
			pgsDirectPlay = userPreferences[UserPreferences.pgsDirectPlay],
		),
		codecLevels = CodecLevelOverrides(
			userAVCLevel = userPreferences[UserPreferences.userAVCLevel].level,
			userHEVCLevel = userPreferences[UserPreferences.userHEVCLevel].level,
		),
		hdr = HdrProfileOptions(
			forceEnabledHdr = userPreferences.getHdrRangeTypesFor(HdrOverrideMode.ENABLE),
			forceDisabledHdr = userPreferences.getHdrRangeTypesFor(HdrOverrideMode.DISABLE),
			displaySupportsDolbyVision = displaySupportsDolbyVision(context),
		),
	),
)

fun createDeviceProfile(
	mediaTest: MediaCodecCapabilitiesTest,
	options: DeviceProfileOptions,
): DeviceProfile = buildDeviceProfile {
	val allowedAudioCodecs = options.audio.allowedCodecs()

	name = "AndroidTV-Default"

	/// Bitrate
	maxStaticBitrate = options.maxBitrate
	maxStreamingBitrate = options.maxBitrate

	addTranscodingProfiles(allowedAudioCodecs, supportsHevc = mediaTest.supportsHevc())
	addDirectPlayProfiles(allowedAudioCodecs)

	/// Codec profiles and HDR exclude list
	addVideoCodecProfiles(mediaTest, options)

	addSubtitleProfiles(options.subtitles)
}

private fun AudioProfileOptions.allowedCodecs(): Array<String> = when {
	downMixAudio -> downmixSupportedAudioCodecs
	else -> supportedAudioCodecs
}.filter { supportedPassthroughAudioCodecs ->
	when (supportedPassthroughAudioCodecs) {
		Codec.Audio.AC3 -> isAC3PrefEnabled
		Codec.Audio.EAC3 -> isEAC3PrefEnabled
		Codec.Audio.TRUEHD -> isTrueHDPrefEnabled
		Codec.Audio.DTS, Codec.Audio.DCA -> isDTSPrefEnabled
		else -> true
	}
}.toTypedArray()

/// Transcoding profiles
private fun DeviceProfileBuilder.addTranscodingProfiles(allowedAudioCodecs: Array<String>, supportsHevc: Boolean) {
	// Video
	val hlsVideoCodecs = listOfNotNull(
		if (supportsHevc) Codec.Video.HEVC else null,
		Codec.Video.H264
	)

	transcodingProfile {
		type = DlnaProfileType.VIDEO
		context = EncodingContext.STREAMING

		container = Codec.Container.TS
		protocol = MediaStreamProtocol.HLS

		// The builder appends each call, so adding the codecs one by one equals one vararg call (and avoids the array copy)
		hlsVideoCodecs.forEach { videoCodec(it) }
		hlsMpegTsAudioCodecs.filter(allowedAudioCodecs::contains).forEach { audioCodec(it) }

		copyTimestamps = false
		enableSubtitlesInManifest = true
	}

	transcodingProfile {
		type = DlnaProfileType.VIDEO
		context = EncodingContext.STREAMING

		container = Codec.Container.MP4
		protocol = MediaStreamProtocol.HLS

		// The builder appends each call, so adding the codecs one by one equals one vararg call (and avoids the array copy)
		hlsVideoCodecs.forEach { videoCodec(it) }
		hlsFmp4AudioCodecs.filter(allowedAudioCodecs::contains).forEach { audioCodec(it) }

		copyTimestamps = false
		enableSubtitlesInManifest = true
	}

	// Audio
	transcodingProfile {
		type = DlnaProfileType.AUDIO
		context = EncodingContext.STREAMING

		container = Codec.Container.TS
		protocol = MediaStreamProtocol.HLS

		audioCodec(Codec.Audio.AAC)
	}
}

/// Direct play profiles
private fun DeviceProfileBuilder.addDirectPlayProfiles(allowedAudioCodecs: Array<String>) {
	// Video
	directPlayProfile {
		type = DlnaProfileType.VIDEO

		container(
			Codec.Container.ASF,
			Codec.Container.HLS,
			Codec.Container.M4V,
			Codec.Container.MKV,
			Codec.Container.MOV,
			Codec.Container.MP4,
			Codec.Container.OGM,
			Codec.Container.OGV,
			Codec.Container.TS,
			Codec.Container.VOB,
			Codec.Container.WEBM,
			Codec.Container.WMV,
			Codec.Container.XVID,
		)

		videoCodec(
			Codec.Video.AV1,
			Codec.Video.H264,
			Codec.Video.HEVC,
			Codec.Video.MPEG,
			Codec.Video.MPEG2VIDEO,
			Codec.Video.VC1,
			Codec.Video.VP8,
			Codec.Video.VP9,
		)

		allowedAudioCodecs.forEach { audioCodec(it) }
	}

	// Audio
	directPlayProfile {
		type = DlnaProfileType.AUDIO

		allowedAudioCodecs.forEach { audioCodec(it) }
	}
}

/// Subtitle profiles
private fun DeviceProfileBuilder.addSubtitleProfiles(options: SubtitleProfileOptions) {
	// Jellyfin server only supports WebVTT subtitles in HLS, other text subtitles will be converted to WebVTT
	// which we do not want so only allow delivery over HLS for WebVTT subtitles
	subtitleProfile(Codec.Subtitle.VTT, embedded = true, hls = true, external = true)
	subtitleProfile(Codec.Subtitle.WEBVTT, embedded = true, hls = true, external = true)

	subtitleProfile(Codec.Subtitle.SRT, embedded = true, external = true)
	subtitleProfile(Codec.Subtitle.SUBRIP, embedded = true, external = true)
	subtitleProfile(Codec.Subtitle.TTML, embedded = true, external = true)

	// Not all subtitles can be loaded standalone by the player
	subtitleProfile(Codec.Subtitle.DVBSUB, embedded = true, encode = true)
	subtitleProfile(Codec.Subtitle.DVDSUB, embedded = true, encode = true)
	subtitleProfile(Codec.Subtitle.IDX, embedded = true, encode = true)
	subtitleProfile(Codec.Subtitle.PGS, embedded = options.pgsDirectPlay, encode = true)
	subtitleProfile(Codec.Subtitle.PGSSUB, embedded = options.pgsDirectPlay, encode = true)

	// ASS/SSA is supported via libass extension
	subtitleProfile(Codec.Subtitle.ASS, encode = true, embedded = options.assDirectPlay, external = options.assDirectPlay)
	subtitleProfile(Codec.Subtitle.SSA, encode = true, embedded = options.assDirectPlay, external = options.assDirectPlay)
}

// Little helper function to more easily define subtitle profiles
private fun DeviceProfileBuilder.subtitleProfile(
	format: String,
	embedded: Boolean = false,
	external: Boolean = false,
	hls: Boolean = false,
	encode: Boolean = false,
) {
	if (embedded) subtitleProfile(format, SubtitleDeliveryMethod.EMBED)
	if (external) subtitleProfile(format, SubtitleDeliveryMethod.EXTERNAL)
	if (hls) subtitleProfile(format, SubtitleDeliveryMethod.HLS)
	if (encode) subtitleProfile(format, SubtitleDeliveryMethod.ENCODE)
}
