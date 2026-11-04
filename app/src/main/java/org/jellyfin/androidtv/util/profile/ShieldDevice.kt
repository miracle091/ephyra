package org.jellyfin.androidtv.util.profile

import android.os.Build

/**
 * Ephyra: tells an NVIDIA Shield TV apart from other Android TV devices. Every model (2015, 2017, 2019 tube and Pro) reports the same
 * MODEL; DEVICE tells them apart (foster, darcy, sif, mdarcy). Use it only for defaults and hints, never to override what MediaCodecList
 * reports.
 */
object ShieldDevice {
	private const val MANUFACTURER = "NVIDIA"
	private const val MODEL = "SHIELD Android TV"

	fun isShieldTv(manufacturer: String = Build.MANUFACTURER, model: String = Build.MODEL): Boolean =
		manufacturer.equals(MANUFACTURER, ignoreCase = true) && model == MODEL
}
