package org.jellyfin.androidtv.benchmark

import java.util.Locale

/** Ephyra (debug builds only): what one clip did in the decoder pass. */
data class DecoderResult(
	val clip: String,
	val decoder: String? = null,
	val hardware: Boolean? = null,
	val firstFrameMs: Long? = null,
	val renderedFrames: Int = 0,
	val droppedFrames: Int = 0,
	val rebuffers: Int = 0,
	val error: String? = null,
) {
	/** Dropped frames as a share of all decoded frames, 0 when nothing was decoded. */
	val droppedShare: Double
		get() = (renderedFrames + droppedFrames).takeIf { it > 0 }?.let { droppedFrames.toDouble() / it } ?: 0.0

	/** Smooth: no error, no rebuffering and under 1% dropped frames. */
	val smooth: Boolean get() = error == null && rebuffers == 0 && renderedFrames > 0 && droppedShare < SMOOTH_DROPPED_SHARE

	private companion object {
		const val SMOOTH_DROPPED_SHARE = 0.01
	}
}

/** The decoder pass as a Markdown table, in the style of the device profile report. */
fun decoderReport(results: List<DecoderResult>): String = buildString {
	appendLine("| Clip | Decoder | Hardware | First frame | Rendered | Dropped | Rebuffers | Result |")
	appendLine("|---|---|---|---|---|---|---|---|")
	for (result in results) {
		val hardware = when (result.hardware) {
			true -> "yes"
			false -> "no"
			null -> "?"
		}
		val firstFrame = result.firstFrameMs?.let { "$it ms" } ?: "-"
		val dropped = "${result.droppedFrames} (${String.format(Locale.ROOT, "%.1f", result.droppedShare * PERCENT)}%)"
		val outcome = result.error?.let { "error: $it" } ?: if (result.smooth) "smooth" else "not smooth"
		appendLine(
			"| ${result.clip} | ${result.decoder ?: "-"} | $hardware | $firstFrame | ${result.renderedFrames} | $dropped | " +
				"${result.rebuffers} | $outcome |"
		)
	}
}

private const val PERCENT = 100
