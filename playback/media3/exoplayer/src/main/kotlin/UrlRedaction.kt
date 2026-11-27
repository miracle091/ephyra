package org.jellyfin.playback.media3.exoplayer

private val API_KEY_PARAMETER = Regex("([?&](?:api_key|apikey)=)[^&#]*", RegexOption.IGNORE_CASE)

/**
 * Velella: replaces the value of the `api_key` and `ApiKey` query parameters (any case) with `***`, so stream URLs can be logged without
 * leaking the access token. The rest of the URL is kept because it is what makes the log useful.
 */
fun String.redactApiKey(): String = replace(API_KEY_PARAMETER, "$1***")

private val SCHEME_AND_HOST = Regex("^[a-z][a-z0-9+.-]*://[^/?#]*", RegexOption.IGNORE_CASE)

/**
 * Velella: a stream URL for the log. A server URL (it carries the access token) keeps everything but the token; any other URL, like
 * a remote live or `.strm` source that may hold credentials in its path or query, is cut to its scheme and host.
 */
fun String.redactForLog(): String =
	if (API_KEY_PARAMETER.containsMatchIn(this)) redactApiKey() else SCHEME_AND_HOST.find(this)?.value ?: "<stream>"
