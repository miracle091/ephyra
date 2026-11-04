package org.jellyfin.androidtv.integration

import android.content.Context
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.preference.HintPreferences
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.util.profile.ShieldDevice

/**
 * Ephyra: on a Shield TV connected to an HDR display, shows once on the home screen where to turn on the Shield's own content
 * matching, a system setting the app can neither read nor change.
 */
class ShieldDisplayHint(
	private val context: Context,
	private val hintPreferences: HintPreferences,
	private val notificationsRepository: NotificationsRepository,
	private val isShieldTv: () -> Boolean = { ShieldDevice.isShieldTv() },
	private val displaySupportsHdr: () -> Boolean = { displaySupportsHdr(context) },
) {
	fun showIfNeeded() {
		if (hintPreferences[HintPreferences.shieldDisplayHintDismissed] || !isShieldTv() || !displaySupportsHdr()) return

		notificationsRepository.addNotification(context.getString(R.string.ephyra_shield_display_hint)) {
			hintPreferences[HintPreferences.shieldDisplayHintDismissed] = true
		}
	}

	private companion object {
		fun displaySupportsHdr(context: Context): Boolean {
			val display = ContextCompat.getDisplayOrDefault(context)

			@Suppress("DEPRECATION")
			val hdrTypes = if (AndroidVersion.isAtLeastUpsideDownCake) display.mode.supportedHdrTypes
			else display.hdrCapabilities?.supportedHdrTypes

			return hdrTypes?.isNotEmpty() == true
		}
	}
}
