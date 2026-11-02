package org.jellyfin.androidtv

/**
 * How Ephyra names itself to the user, the Jellyfin server and in reports.
 */
object EphyraInfo {
	/** Client name sent to the server; it shows in the server's device list and activity log. */
	const val CLIENT_NAME = "Ephyra"

	/** Prefix of the version line shown in the app, as in "ephyra 1.0.0 release". */
	const val VERSION_PREFIX = "ephyra"

	/** Repository linked from crash and playback reports. */
	const val REPOSITORY_URL = "https://github.com/miracle091/ephyra"
}
