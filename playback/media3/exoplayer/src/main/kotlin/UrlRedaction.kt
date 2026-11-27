package org.jellyfin.playback.media3.exoplayer

private val API_KEY_PARAMETER = Regex("([?&](?:api_key|apikey)=)[^&#]*", RegexOption.IGNORE_CASE)

/**
 * Velella: replaces the value of the `api_key` and `ApiKey` query parameters (any case) with `***`, so stream URLs can be logged without
 * leaking the access token. The rest of the URL is kept because it is what makes the log useful.
 */
fun String.redactApiKey(): String = replace(API_KEY_PARAMETER, "$1***")
