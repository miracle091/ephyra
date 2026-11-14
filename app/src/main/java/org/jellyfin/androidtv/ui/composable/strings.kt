package org.jellyfin.androidtv.ui.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R

/**
 * A resolution label, used when either the width or the height reaches its minimum.
 * When [appendScanSuffix] is set, the scan suffix (i or p) is appended to the [label].
 */
private class ResolutionTier(
	val minWidth: Int,
	val minHeight: Int,
	val label: String,
	val appendScanSuffix: Boolean,
)

private val resolutionTiers = listOf(
	ResolutionTier(minWidth = 7600, minHeight = 4300, label = "8K", appendScanSuffix = false),
	ResolutionTier(minWidth = 3800, minHeight = 2000, label = "4K", appendScanSuffix = false),
	ResolutionTier(minWidth = 2500, minHeight = 1400, label = "1440", appendScanSuffix = true),
	ResolutionTier(minWidth = 1800, minHeight = 1000, label = "1080", appendScanSuffix = true),
	ResolutionTier(minWidth = 1200, minHeight = 700, label = "720", appendScanSuffix = true),
	ResolutionTier(minWidth = 600, minHeight = 400, label = "480", appendScanSuffix = true),
)

@Composable
fun getResolutionName(width: Int, height: Int, interlaced: Boolean = false): String {
	val tier = resolutionTiers.firstOrNull { width >= it.minWidth || height >= it.minHeight }
		?: return stringResource(R.string.lbl_sd)

	return if (tier.appendScanSuffix) tier.label + if (interlaced) "i" else "p" else tier.label
}
