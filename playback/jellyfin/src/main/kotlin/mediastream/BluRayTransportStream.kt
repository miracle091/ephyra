@file:JvmName("BluRayTransportStream")

package org.jellyfin.playback.jellyfin.mediastream

private val bluRayTransportStreamExtensions = setOf("m2ts", "mts")

/**
 * Velella: whether the file is a Blu-ray transport stream (192-byte packets, `.m2ts`/`.mts`). The server reports it as `ts` like a
 * plain transport stream, but Media3's TS extractor only reads 188-byte packets, so direct play fails before the server takes
 * over: both players send these files to the server from the start.
 */
fun isBluRayTransportStream(path: String?): Boolean =
	path?.substringAfterLast('.', "")?.lowercase() in bluRayTransportStreamExtensions
