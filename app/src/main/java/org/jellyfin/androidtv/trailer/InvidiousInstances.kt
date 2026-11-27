package org.jellyfin.androidtv.trailer

import kotlinx.serialization.json.JsonArray
import kotlin.time.Duration.Companion.hours

/** Velella: the public list of Invidious instances, kept for [INSTANCE_LIST_MAX_AGE]. */
const val INVIDIOUS_INSTANCES_URL = "https://api.invidious.io/instances.json"
private val INSTANCE_LIST_MAX_AGE = 24.hours

/** Velella: instances tried after the public list, for when it cannot be fetched: the ones that had their API open. */
val InvidiousFallbackInstances = listOf("https://yt.chocolatemoo53.com")

/** Velella: the HTTPS instances of the public list ([INVIDIOUS_INSTANCES_URL]) with their API open, without the trailing slash. */
fun parseInvidiousInstances(json: String): List<String> {
	val entries = parseJson(json) as? JsonArray ?: return emptyList()
	return entries.mapNotNull { entry ->
		val info = (entry as? JsonArray)?.getOrNull(1) ?: return@mapNotNull null
		val uri = info.string("uri") ?: return@mapNotNull null
		uri.trimEnd('/').takeIf { info.string("type") == "https" && info.boolean("api") == true && it.startsWith("https://") }
	}
}

/** Velella: [address] as an instance base URL: HTTPS when it names no scheme, no trailing slash; null when blank. */
fun normalizeInvidiousInstance(address: String?): String? {
	val trimmed = address?.trim()?.trimEnd('/')
	if (trimmed.isNullOrEmpty()) return null
	return if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) trimmed else "https://$trimmed"
}

/** Velella: the instances to try, in order: the user's own, the last one that worked, then the listed ones. */
fun invidiousCandidates(own: String?, lastWorking: String?, listed: List<String>): List<String> =
	listOfNotNull(normalizeInvidiousInstance(own), normalizeInvidiousInstance(lastWorking))
		.plus(listed.mapNotNull(::normalizeInvidiousInstance))
		.distinct()

/** Velella: whether a cached instance list written at [lastModified] (milliseconds) is still used at [now]. */
fun isInstanceListFresh(lastModified: Long, now: Long): Boolean =
	lastModified > 0 && now - lastModified in 0 until INSTANCE_LIST_MAX_AGE.inWholeMilliseconds
