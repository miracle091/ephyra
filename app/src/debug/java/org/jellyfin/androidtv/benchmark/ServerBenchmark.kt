package org.jellyfin.androidtv.benchmark

import org.jellyfin.androidtv.ui.playback.PlayInfo
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.TranscodeReason
import java.net.URLDecoder

/** Ephyra (debug builds only): what the server decides for one clip, when the clip is in one of the user's libraries. */
data class ServerResult(
	val clip: String,
	val playMethod: PlayMethod? = null,
	val transcodeReasons: List<TranscodeReason> = emptyList(),
	val transcodeVideoCodec: String? = null,
	val error: String? = null,
)

/**
 * Ephyra (debug builds only): asks the server how it would deliver each test clip to this device, with the device profile the
 * players send. Nothing is played. The clips must be in a library; the server names them all "Test Jellyfin", so they are
 * matched by file name.
 */
class ServerBenchmark(
	private val api: ApiClient,
	private val deviceProfile: DeviceProfile,
) {
	suspend fun run(clips: List<BenchmarkClip>): List<ServerResult> {
		val items = api.itemsApi.getItems(
			searchTerm = SEARCH_TERM,
			recursive = true,
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.VIDEO),
			fields = setOf(ItemFields.PATH),
		).content.items

		return clips.map { clip ->
			val item = items.firstOrNull { it.path?.substringAfterLast('/')?.substringAfterLast('\\') == clip.fileName }
				?: return@map ServerResult(clip.label, error = "not in a library")

			val response = api.mediaInfoApi.getPostedPlaybackInfo(
				itemId = item.id,
				data = PlaybackInfoDto(
					deviceProfile = deviceProfile,
					enableDirectPlay = true,
					enableDirectStream = true,
					enableTranscoding = true,
					allowVideoStreamCopy = true,
					allowAudioStreamCopy = true,
					autoOpenLiveStream = false,
				),
			).content

			val source = response.mediaSources.firstOrNull()
			when {
				response.errorCode != null -> ServerResult(clip.label, error = response.errorCode?.serialName)
				source == null -> ServerResult(clip.label, error = "no media source")
				else -> source.toResult(clip)
			}
		}
	}

	private fun MediaSourceInfo.toResult(clip: BenchmarkClip): ServerResult {
		val playMethod = when {
			supportsDirectPlay -> PlayMethod.DIRECT_PLAY
			supportsDirectStream -> PlayMethod.DIRECT_STREAM
			else -> PlayMethod.TRANSCODE
		}
		if (playMethod == PlayMethod.DIRECT_PLAY) return ServerResult(clip.label, playMethod)

		return ServerResult(
			clip = clip.label,
			playMethod = playMethod,
			transcodeReasons = PlayInfo.transcodeReasons(transcodingUrl),
			transcodeVideoCodec = transcodingUrl
				?.let { videoCodecParameter.find(it) }
				// The Charset overload of decode needs API 33
				?.let { URLDecoder.decode(it.groupValues[1], "UTF-8") }
				?.substringBefore(','),
		)
	}

	private companion object {
		const val SEARCH_TERM = "Test Jellyfin"
		val videoCodecParameter = Regex("[?&]VideoCodec=([^&]*)", RegexOption.IGNORE_CASE)
	}
}

/** The server pass as a Markdown table. */
fun serverReport(results: List<ServerResult>): String = buildString {
	appendLine("| Clip | Play method | Reasons | Transcoded video |")
	appendLine("|---|---|---|---|")
	for (result in results) {
		val method = result.error?.let { "error: $it" } ?: result.playMethod?.serialName ?: "-"
		val reasons = result.transcodeReasons.joinToString { it.serialName }.ifEmpty { "-" }
		appendLine("| ${result.clip} | $method | $reasons | ${result.transcodeVideoCodec ?: "-"} |")
	}
}
