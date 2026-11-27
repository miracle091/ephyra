package org.jellyfin.androidtv

/**
 * How Velella names itself to the user, the Jellyfin server and in reports.
 */
object VelellaInfo {
	/** Client name sent to the server; it shows in the server's device list and activity log. */
	const val CLIENT_NAME = "Velella"

	/** Prefix of the version line shown in the app, as in "velella 1.0.0 release". */
	const val VERSION_PREFIX = "velella"

	/**
	 * Client id of the display preferences Velella saves on the server (per-library view and sort settings).
	 * Upstream uses "jellyfin-androidtv"; a separate id keeps Velella's settings apart from the official app's.
	 */
	const val DISPLAY_PREFERENCES_CLIENT = "velella"

	/** Repository linked from crash and playback reports. */
	const val REPOSITORY_URL = "https://github.com/miracle091/velella"
}
