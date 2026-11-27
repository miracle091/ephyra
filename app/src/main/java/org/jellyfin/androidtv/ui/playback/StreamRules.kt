package org.jellyfin.androidtv.ui.playback

import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod

/** Velella: small rules the legacy player applies to the stream the server offers. */
object StreamRules {
	/**
	 * The subtitle actually on screen: one the server burns into the video ([SubtitleDeliveryMethod.ENCODE] on [serverIndex])
	 * shows whatever the app picked, so a later restart (an audio switch) must ask for it again or the server drops it.
	 * Otherwise the app's [pickedIndex] stays.
	 */
	@JvmStatic
	fun shownSubtitleIndex(deliveryMethod: SubtitleDeliveryMethod?, serverIndex: Int?, pickedIndex: Int?): Int? =
		if (deliveryMethod == SubtitleDeliveryMethod.ENCODE && serverIndex != null && serverIndex >= 0) serverIndex else pickedIndex
}
