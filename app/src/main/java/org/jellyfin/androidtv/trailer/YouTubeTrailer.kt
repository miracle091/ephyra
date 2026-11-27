package org.jellyfin.androidtv.trailer

import org.jellyfin.sdk.model.api.BaseItemDto
import java.net.URI
import java.net.URISyntaxException

private val YouTubeIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
private val YouTubeHosts = setOf(
	"youtube.com",
	"www.youtube.com",
	"m.youtube.com",
	"youtube-nocookie.com",
	"www.youtube-nocookie.com",
)
private val ShortLinkHosts = setOf("youtu.be", "www.youtu.be")
private val IdPathPrefixes = listOf("/embed/", "/shorts/", "/v/", "/live/")

/**
 * Velella: the YouTube video id in a remote trailer link the server stores, or null for other sites and pages. Only the id is
 * used, to ask an Invidious instance for the video: the app never opens YouTube.
 */
fun youTubeVideoId(url: String): String? {
	val uri = try {
		URI(url.trim())
	} catch (_: URISyntaxException) {
		return null
	}
	val host = uri.host?.lowercase() ?: return null
	val path = uri.path.orEmpty()
	val id = when (host) {
		in ShortLinkHosts -> path.removePrefix("/").substringBefore('/')
		in YouTubeHosts -> when {
			path == "/watch" -> uri.rawQuery.orEmpty().split('&').firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
			else -> IdPathPrefixes.firstOrNull { path.startsWith(it) }?.let { path.removePrefix(it).substringBefore('/') }
		}

		else -> null
	}
	return id?.takeIf { YouTubeIdPattern.matches(it) }
}

/** Velella: the YouTube ids of the item's remote trailers, in the server's order, without repeats; other sites are ignored. */
fun BaseItemDto.youTubeTrailerIds(): List<String> = remoteTrailers.orEmpty()
	.mapNotNull { trailer -> trailer.url?.let(::youTubeVideoId) }
	.distinct()
