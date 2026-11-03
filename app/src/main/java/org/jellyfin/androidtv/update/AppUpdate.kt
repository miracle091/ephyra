package org.jellyfin.androidtv.update

import org.jellyfin.androidtv.EphyraInfo

/** A release of Ephyra published on GitHub Releases. */
data class AppUpdate(
	val versionName: String,
	val versionCode: Int,
) {
	/** The release's APK, published by the release workflow as "ephyra-v<version>.apk" under the tag "v<version>". */
	val apkUrl get() = "${EphyraInfo.REPOSITORY_URL}/releases/download/v$versionName/ephyra-v$versionName.apk"

	companion object {
		/** The latest release's version file, written by the app's versionTxt Gradle task. */
		const val VERSION_TXT_URL = "${EphyraInfo.REPOSITORY_URL}/releases/latest/download/version.txt"

		// Semantic version with an optional pre-release part, as getVersionName() in buildSrc produces it
		private val versionNameRegex = Regex("""\d{1,2}\.\d{1,2}\.\d{1,2}(-[0-9A-Za-z.]+)?""")

		/**
		 * Parses the version file, "v1.2.3=1020399". Returns null when it is malformed, so a broken release never
		 * becomes a download URL.
		 */
		fun parseVersionTxt(text: String): AppUpdate? {
			val parts = text.trim().split('=')
			if (parts.size != 2) return null

			val versionName = parts[0].removePrefix("v").takeIf(versionNameRegex::matches) ?: return null
			val versionCode = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
			return AppUpdate(versionName, versionCode)
		}
	}
}
