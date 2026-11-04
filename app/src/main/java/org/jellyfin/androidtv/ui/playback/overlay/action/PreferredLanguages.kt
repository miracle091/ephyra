package org.jellyfin.androidtv.ui.playback.overlay.action

import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.UserConfiguration
import org.koin.java.KoinJavaComponent
import java.util.Locale
import java.util.MissingResourceException

/**
 * Ephyra: the audio or subtitle tracks with the user's preferred languages first (the language set on the server for the user,
 * then the TV's language), so a remux with many tracks needs fewer D-pad presses. The other tracks keep their order.
 */
fun List<MediaStream>.preferredLanguagesFirst(serverPreference: (UserConfiguration) -> String?): List<MediaStream> {
	val configuration = KoinJavaComponent.get<UserRepository>(UserRepository::class.java).currentUser.value?.configuration
	val deviceLanguage = try {
		Locale.getDefault().isO3Language
	} catch (_: MissingResourceException) {
		null
	}

	return preferredLanguagesFirst(listOfNotNull(configuration?.let(serverPreference), deviceLanguage)) { it.language }
}

/** Items whose language is in [preferred] first, in the order of [preferred]; the sort is stable, so the rest keep their order. */
internal fun <T> List<T>.preferredLanguagesFirst(preferred: List<String>, language: (T) -> String?): List<T> {
	val ranks = preferred.mapNotNull(::normalizeLanguage).distinct()
	if (ranks.isEmpty()) return this

	return sortedBy { item ->
		val rank = language(item)?.let(::normalizeLanguage)?.let(ranks::indexOf) ?: -1
		if (rank >= 0) rank else ranks.size
	}
}

/** A language code as lowercase ISO 639-2/T ("ger" and "de" become "deu"), or null when blank. */
private fun normalizeLanguage(code: String): String? {
	val lowercase = code.trim().lowercase(Locale.ROOT).ifEmpty { return null }
	if (lowercase.length == 2) {
		return try {
			Locale.forLanguageTag(lowercase).isO3Language.ifEmpty { lowercase }
		} catch (_: MissingResourceException) {
			lowercase
		}
	}
	return bibliographicToTerminology[lowercase] ?: lowercase
}

/** The ISO 639-2 languages whose bibliographic code (used by Jellyfin) differs from the terminology code (used by Java's Locale). */
private val bibliographicToTerminology = mapOf(
	"alb" to "sqi", "arm" to "hye", "baq" to "eus", "bur" to "mya", "chi" to "zho", "cze" to "ces", "dut" to "nld",
	"fre" to "fra", "geo" to "kat", "ger" to "deu", "gre" to "ell", "ice" to "isl", "mac" to "mkd", "mao" to "mri",
	"may" to "msa", "per" to "fas", "rum" to "ron", "slo" to "slk", "tib" to "bod", "wel" to "cym",
)
