package org.jellyfin.androidtv.util.profile

import android.content.Context
import android.media.MediaCodecList
import android.util.Range
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.EphyraInfo
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.util.appendCodeBlock
import org.jellyfin.androidtv.util.appendDetails
import org.jellyfin.androidtv.util.buildMarkdown
import org.jellyfin.sdk.api.client.util.ApiSerializer
import org.jellyfin.sdk.model.ServerVersion

private val prettyPrintJson = Json { prettyPrint = true }
private fun formatJson(json: String) = prettyPrintJson.encodeToString(prettyPrintJson.parseToJsonElement(json))

internal fun Range<Int>.prettyFormat() = if (lower == upper) "$lower" else "$lower-$upper"

// HDR formats to label strings
enum class HdrFormats(val label: String) {
	DOLBY_VISION("Dolby Vision"),
	DOLBY_VISION_EL("Dolby Vision Enhancement Layer"),
	HDR10("HDR10"),
	HDR10_PLUS("HDR10+"),
	HLG("HLG"),
}

fun createDeviceProfileReport(
	context: Context,
	userPreferences: UserPreferences,
	serverVersion: ServerVersion,
) = buildMarkdown {
	// Header
	appendLine("---")
	appendLine("client: ${EphyraInfo.CLIENT_NAME}")
	appendLine("client_version: ${BuildConfig.VERSION_NAME}")
	appendLine("client_repository: ${EphyraInfo.REPOSITORY_URL}")
	appendLine("type: media_capabilities_report")
	appendLine("format: markdown")
	appendLine("---")
	appendLine()

	// Device profile send to server
	appendDetails("Generated device profile") {
		appendLine("- Server compatibility: $serverVersion")
		appendCodeBlock(
			language = "json",
			code = createDeviceProfile(context, userPreferences)
				.let(ApiSerializer::encodeRequestBody)
				?.let(::formatJson)
		)
	}

	// Device capabilities used to generate profile
	val codecs = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
		.filter { !it.isEncoder }
		.sortedBy { if (AndroidVersion.isAtLeastQ) it.canonicalName else it.name }

	appendDetails("Device codec decoders") { appendCodecDecoders(codecs) }
	appendDetails("Audio passthrough capabilities") { appendPassthroughCapabilities(context) }
	appendDetails("Known media types") { appendKnownMediaTypes(codecs) }
	appendDetails("Codec HDR Support") { appendCodecHdrSupport(userPreferences) }
	appendDetails("Display information") { appendDisplayInformation(context) }

	appendAppInformation(context)
	appendDeviceInformation()
}
