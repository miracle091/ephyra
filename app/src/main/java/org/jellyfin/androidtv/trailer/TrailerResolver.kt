package org.jellyfin.androidtv.trailer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jellyfin.androidtv.preference.VelellaPreferences
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val INSTANCE_TIMEOUT_SECONDS = 5L
private const val MAX_ANSWER_BYTES = 4L * 1024 * 1024

/** Velella: a trailer's streams and the Invidious instance that gave them. */
data class ResolvedTrailer(val instance: String, val streams: TrailerStreams)

/**
 * Velella: finds a trailer's streams on Invidious, never on YouTube. Nothing is fetched until a trailer is asked for: then the
 * public instance list (cached for a day in [cacheFile]), and the instances in turn, the user's own first, each given 5 seconds;
 * the one that answers is remembered for next time.
 */
class TrailerResolver(
	private val preferences: VelellaPreferences,
	baseClient: OkHttpClient,
	private val cacheFile: File,
) {
	private val client = baseClient.newBuilder()
		.callTimeout(INSTANCE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
		.build()

	/** The streams of the YouTube video [videoId] from the first instance that has them, skipping [skip]; null when none has. */
	suspend fun resolve(videoId: String, skip: Set<String> = emptySet()): ResolvedTrailer? = withContext(Dispatchers.IO) {
		val candidates = invidiousCandidates(
			own = preferences[VelellaPreferences.invidiousInstance],
			lastWorking = preferences[VelellaPreferences.invidiousLastInstance],
			listed = instances(),
		).filterNot { it in skip }

		for (instance in candidates) {
			val streams = try {
				fetch("$instance/api/v1/videos/$videoId?local=true&fields=adaptiveFormats,formatStreams")
					?.let { parseTrailerStreams(it, instance) }
			} catch (error: CancellationException) {
				throw error
			} catch (error: IOException) {
				Timber.w(error, "Invidious instance %s did not answer", instance)
				null
			}

			if (streams != null) {
				preferences[VelellaPreferences.invidiousLastInstance] = instance
				return@withContext ResolvedTrailer(instance, streams)
			}
			Timber.i("Invidious instance %s has no playable streams for the trailer", instance)
		}
		null
	}

	/** The public instances, from the cache while it is fresh, else from the list (kept stale when that fails), then the fallback. */
	private fun instances(): List<String> {
		val cached = cacheFile.takeIf { it.exists() }
		val listed = if (cached != null && isInstanceListFresh(cached.lastModified(), System.currentTimeMillis())) {
			parseInvidiousInstances(cached.readText())
		} else {
			val fetched = try {
				fetch(INVIDIOUS_INSTANCES_URL)
			} catch (error: IOException) {
				Timber.w(error, "Could not fetch the Invidious instance list")
				null
			}
			fetched?.let { json ->
				parseInvidiousInstances(json).also { if (it.isNotEmpty()) cacheFile.writeText(json) }
			} ?: cached?.let { parseInvidiousInstances(it.readText()) }.orEmpty()
		}
		return listed + InvidiousFallbackInstances
	}

	/** The body of a successful JSON answer at [url], or null; at most a few megabytes. */
	private fun fetch(url: String): String? {
		val request = Request.Builder().url(url).header("Accept", "application/json").build()
		return client.newCall(request).execute().use { response ->
			val body = response.body
			if (!response.isSuccessful || body == null || body.contentLength() > MAX_ANSWER_BYTES) return@use null
			val source = body.source()
			source.request(MAX_ANSWER_BYTES + 1)
			if (source.buffer.size > MAX_ANSWER_BYTES) null else source.buffer.readUtf8()
		}
	}
}
