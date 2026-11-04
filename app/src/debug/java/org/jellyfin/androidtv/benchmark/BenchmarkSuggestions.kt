package org.jellyfin.androidtv.benchmark

import org.jellyfin.sdk.model.api.PlayMethod

/**
 * Ephyra (debug builds only): settings suggested by the measurements, each with the numbers behind it. Never changes a setting:
 * the user decides.
 */
fun benchmarkSuggestions(decoder: List<DecoderResult>, server: List<ServerResult>): List<String> = buildList {
	val av1Hd = decoder.filter { "AV1" in it.clip && it.clip.startsWith("1080p") }
	val av1Uhd = decoder.filter { "AV1" in it.clip && it.clip.startsWith("4K") }
	val dolbyVision = decoder.filter { " DV " in " ${it.clip} " }

	if (av1Hd.isNotEmpty()) {
		val clips = av1Hd.joinToString { "${it.clip}: ${it.outcome}" }
		add(
			if (av1Hd.all { it.smooth }) "Software AV1 up to 1080p is smooth ($clips): turning software codecs on lets 1080p AV1 play directly."
			else "Software AV1 at 1080p is not smooth ($clips): keep software codecs off, so the server transcodes AV1."
		)
	}

	if (av1Uhd.isNotEmpty() && av1Uhd.none { it.smooth }) {
		add("4K AV1 is not smooth in software (${av1Uhd.joinToString { "${it.clip}: ${it.outcome}" }}): keep the 1080p cap.")
	}

	if (dolbyVision.isNotEmpty()) {
		val clips = dolbyVision.joinToString { "${it.clip}: ${it.outcome}" }
		if (dolbyVision.all { it.smooth && it.hardware == true }) add("Dolby Vision plays on a hardware decoder ($clips).")
		else add("Dolby Vision does not play smoothly in hardware ($clips): set the Dolby Vision override to HDR10 fallback.")
	}

	val transcodedToH264 = server.filter {
		it.playMethod == PlayMethod.TRANSCODE && it.transcodeVideoCodec.equals("h264", ignoreCase = true)
	}
	if (transcodedToH264.isNotEmpty()) {
		add(
			"The server transcodes to H.264 (${transcodedToH264.joinToString { it.clip }}): allow HEVC encoding in the server's " +
				"transcoding settings, so HDR and 4K survive the transcode."
		)
	}
}

/** "smooth", "n% dropped, m rebuffers" or the error, for the suggestion texts. */
val DecoderResult.outcome: String
	get() = when {
		error != null -> "error $error"
		smooth -> "smooth"
		else -> "${droppedFrames}/${renderedFrames + droppedFrames} dropped, $rebuffers rebuffers"
	}
