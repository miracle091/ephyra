package org.jellyfin.androidtv.trailer

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val MAX_TRAILER_HEIGHT = 1080
private const val AAC_ITAG = "140"

private val LenientJson = Json { ignoreUnknownKeys = true }

/** Velella: the video and, for a video-only stream, the audio to play together for a trailer. */
data class TrailerStreams(val videoUrl: String, val audioUrl: String?)

internal fun parseJson(json: String): JsonElement? = try {
	LenientJson.parseToJsonElement(json)
} catch (_: SerializationException) {
	null
} catch (_: IllegalArgumentException) {
	null
}

internal fun JsonElement?.string(key: String): String? = ((this as? JsonObject)?.get(key) as? JsonPrimitive)
	?.contentOrNull

internal fun JsonElement?.boolean(key: String): Boolean? = ((this as? JsonObject)?.get(key) as? JsonPrimitive)
	?.booleanOrNull

private fun JsonElement.height(): Int? = string("resolution")?.substringBefore('p')?.toIntOrNull()
private fun JsonElement.type(): String = string("type").orEmpty()
private fun JsonElement.bitrate(): Long = string("bitrate")?.toLongOrNull() ?: 0L

/** Velella: the hosts of YouTube and Google's video servers; a trailer stream never comes from them. */
private val YouTubeHosts = listOf(
	"googlevideo.com", "youtube.com", "youtu.be", "youtube-nocookie.com", "ytimg.com", "ggpht.com", "googleusercontent.com", "google.com",
)

/** Velella: whether [host] is (a subdomain of) one of YouTube's or Google's video hosts. */
fun isYouTubeHost(host: String): Boolean {
	val name = host.lowercase().trimEnd('.')
	return YouTubeHosts.any { name == it || name.endsWith(".$it") }
}

/**
 * Velella: [url] from an instance's answer as a full address, relative ones against [instance]. Null when it is missing or
 * malformed, or when it points at YouTube: an instance that doesn't proxy the streams (`local=true` is only a request) is skipped.
 */
private fun resolveUrl(instance: String, url: String?): String? {
	if (url.isNullOrBlank()) return null
	val resolved = "$instance/".toHttpUrlOrNull()?.resolve(url.removePrefix("/")) ?: return null
	return resolved.takeUnless { isYouTubeHost(it.host) }?.toString()
}

/**
 * Velella: the streams to play from an Invidious `/api/v1/videos/<id>` answer: the best AVC video up to 1080p with the AAC audio
 * (the default track, itag 140 first), which every Shield decodes in hardware; else the best progressive MP4. Null when the
 * answer is an error or has neither.
 */
fun parseTrailerStreams(json: String, instance: String): TrailerStreams? {
	val video = parseJson(json) as? JsonObject ?: return null
	val adaptive = (video["adaptiveFormats"] as? JsonArray).orEmpty()

	val avc = adaptive
		.filter { it.type().startsWith("video/mp4") && "avc1" in it.type() && (it.height() ?: 0) <= MAX_TRAILER_HEIGHT }
		.maxWithOrNull(compareBy<JsonElement>({ it.height() ?: 0 }, { it.bitrate() }))
	val aac = adaptive
		.filter { it.type().startsWith("audio/mp4") && "mp4a" in it.type() }
		.maxWithOrNull(
			compareBy<JsonElement>(
				{ (it as? JsonObject)?.get("audioTrack").boolean("audioIsDefault") != false },
				{ it.string("itag") == AAC_ITAG },
				{ it.bitrate() },
			)
		)
	val videoUrl = resolveUrl(instance, avc?.string("url"))
	val audioUrl = resolveUrl(instance, aac?.string("url"))
	if (videoUrl != null && audioUrl != null) return TrailerStreams(videoUrl, audioUrl)

	val progressive = (video["formatStreams"] as? JsonArray).orEmpty()
		.filter { it.type().startsWith("video/mp4") }
		.maxByOrNull { it.height() ?: 0 }
	return resolveUrl(instance, progressive?.string("url"))?.let { TrailerStreams(it, null) }
}
