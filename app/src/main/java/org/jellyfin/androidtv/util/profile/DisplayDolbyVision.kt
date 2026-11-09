package org.jellyfin.androidtv.util.profile

import android.content.Context
import android.view.Display
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.util.AndroidVersion

/**
 * Ephyra: true when [hdrTypes] (Display.HdrCapabilities.getSupportedHdrTypes) lists Dolby Vision. No list means no HDR at all.
 */
internal fun hdrTypesIncludeDolbyVision(hdrTypes: IntArray?): Boolean =
	hdrTypes?.contains(Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION) == true

/**
 * Ephyra: whether the connected display accepts Dolby Vision. The decoder list says what the device can decode, not what the TV can show:
 * a Dolby Vision decoder in front of a display without Dolby Vision gave an SDR picture on the Shield (upstream issue #5794).
 */
fun displaySupportsDolbyVision(context: Context): Boolean {
	val display = ContextCompat.getDisplayOrDefault(context)

	@Suppress("DEPRECATION")
	val hdrTypes = if (AndroidVersion.isAtLeastUpsideDownCake) display.mode.supportedHdrTypes
	else display.hdrCapabilities?.supportedHdrTypes

	return hdrTypesIncludeDolbyVision(hdrTypes)
}
