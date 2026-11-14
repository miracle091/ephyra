package org.jellyfin.androidtv.util.profile

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.Surface
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.util.MarkdownBuilder
import org.jellyfin.androidtv.util.appendItem
import org.jellyfin.androidtv.util.appendSection
import org.jellyfin.androidtv.util.appendValue
import kotlin.time.Duration.Companion.nanoseconds

internal fun MarkdownBuilder.appendDisplayInformation(context: Context) {
	val display = ContextCompat.getDisplayOrDefault(context)

	// Basic information
	appendItem("Id") { appendValue(display.displayId.toString()) }
	appendItem("Name") { appendValue(display.name) }
	if (AndroidVersion.isAtLeastS && display.deviceProductInfo != null) {
		val productInfo = requireNotNull(display.deviceProductInfo)
		appendItem("Display product id") { appendValue(productInfo.productId) }
		appendItem("Display bame") { appendValue(productInfo.name) }
		appendItem("Display manufacture year") { appendValue(productInfo.manufactureYear.toString()) }
		appendItem("Display manufacture week") { appendValue(productInfo.manufactureWeek.toString()) }
		appendItem("Display manufacturer pnp id") { appendValue(productInfo.manufacturerPnpId) }
		appendItem("Display model year") { appendValue(productInfo.modelYear.toString()) }
	}
	appendItem("Rotation") {
		when (display.rotation) {
			Surface.ROTATION_0 -> append("0°")
			Surface.ROTATION_90 -> append("90°")
			Surface.ROTATION_180 -> append("180°")
			Surface.ROTATION_270 -> append("270°")
			else -> appendValue("Unknown (${display.rotation}")
		}
	}

	// Refresh rate and timing
	appendItem("Refresh rate") { appendValue(display.refreshRate.toString()) }
	if (AndroidVersion.isAtLeastBaklava) appendItem("Adaptive refresh rate") { appendValue(display.hasArrSupport().toString()) }
	appendItem("VSYNC offset") { appendValue(display.appVsyncOffsetNanos.nanoseconds.toString()) }
	appendItem("Presentation deadline") { appendValue(display.presentationDeadlineNanos.nanoseconds.toString()) }
	if (AndroidVersion.isAtLeastR) appendItem("Minimal post processing") { appendValue(display.isMinimalPostProcessingSupported.toString()) }

	appendDisplayHdr(display)
}

private fun MarkdownBuilder.appendDisplayHdr(display: Display) {
	if (AndroidVersion.isAtLeastO) appendItem("Any HDR") { appendValue(display.isHdr.toString()) }
	if (AndroidVersion.isAtLeastO) appendItem("Wide color gamut") { appendValue(display.isWideColorGamut.toString()) }
	if (AndroidVersion.isAtLeastQ) appendItem("Preferred wide color space") { appendValue(display.preferredWideGamutColorSpace.toString()) }
	if (AndroidVersion.isAtLeastN) {
		@Suppress("DEPRECATION")
		val supportedHdrTypes = if (AndroidVersion.isAtLeastUpsideDownCake) display.mode.supportedHdrTypes.toList()
		else display.hdrCapabilities?.supportedHdrTypes?.toList().orEmpty()

		appendItem("HDR capabilities") {
			appendLine()
			appendLine("- ${HdrFormats.DOLBY_VISION.label}: ${supportedHdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION)}")
			appendLine("- ${HdrFormats.HDR10}: ${supportedHdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10)}")
			if (AndroidVersion.isAtLeastQ) {
				appendLine("- ${HdrFormats.HDR10_PLUS.label}: ${supportedHdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS)}")
			}
			appendLine("- ${HdrFormats.HLG.label}: ${supportedHdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HLG)}")
		}
	}

	if (AndroidVersion.isAtLeastUpsideDownCake) appendItem("HDR/SDR ratio") {
		appendLine()
		appendItem("Available") { appendValue(display.isHdrSdrRatioAvailable.toString()) }
		appendItem("Ratio") { appendValue(display.hdrSdrRatio.toString()) }
		if (AndroidVersion.isAtLeastBaklava) {
			appendItem("Highest ratio") { appendValue(display.highestHdrSdrRatio.toString()) }
		}
	}
}

internal fun MarkdownBuilder.appendAppInformation(context: Context) = appendSection("App information") {
	appendItem("App version") {
		appendValue(BuildConfig.VERSION_NAME)
		append(" (")
		appendValue(BuildConfig.VERSION_CODE.toString())
		append(")")
	}
	appendItem("Package name") { appendValue(context.packageName) }
}

internal fun MarkdownBuilder.appendDeviceInformation() = appendSection("Device information") {
	appendItem("Android version") { appendValue(Build.VERSION.RELEASE) }
	appendItem("Device brand") { appendValue(Build.BRAND) }
	appendItem("Device product") { appendValue(Build.PRODUCT) }
	appendItem("Device model") { appendValue(Build.MODEL) }
	appendItem("Device manufacturer") { appendValue(Build.MANUFACTURER) }
	appendItem("Device codename") { appendValue(Build.DEVICE) }
	if (AndroidVersion.isAtLeastS) appendItem("Device SKU") { appendValue(Build.SKU) }
	if (AndroidVersion.isAtLeastS) appendItem("Device SOC") { appendValue(Build.SOC_MODEL) }
}
