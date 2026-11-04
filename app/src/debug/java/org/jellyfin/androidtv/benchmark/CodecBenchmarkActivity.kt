package org.jellyfin.androidtv.benchmark

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.profile.createDeviceProfile
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.clientLogApi
import org.jellyfin.sdk.model.ServerVersion
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Ephyra (debug builds only): plays each of Jellyfin's test clips for a few seconds and reports which decoder ran and how
 * smoothly, so the settings can be chosen from measurements on this device. Opened from the developer settings.
 */
class CodecBenchmarkActivity : FragmentActivity() {
	private val api by inject<ApiClient>()
	private val userPreferences by inject<UserPreferences>()

	private lateinit var surfaceView: SurfaceView
	private lateinit var status: TextView
	private lateinit var report: TextView
	private lateinit var reportScroll: ScrollView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		surfaceView = SurfaceView(this)
		status = TextView(this).apply {
			setTextColor(Color.WHITE)
			setShadowLayer(SHADOW_RADIUS, 0f, 0f, Color.BLACK)
			textSize = STATUS_TEXT_SIZE
			setPadding(OVERSCAN_PADDING, OVERSCAN_PADDING, OVERSCAN_PADDING, OVERSCAN_PADDING)
		}
		report = TextView(this).apply {
			setTextColor(Color.WHITE)
			typeface = android.graphics.Typeface.MONOSPACE
			textSize = REPORT_TEXT_SIZE
			setPadding(OVERSCAN_PADDING, OVERSCAN_PADDING, OVERSCAN_PADDING, OVERSCAN_PADDING)
		}
		reportScroll = ScrollView(this).apply {
			setBackgroundColor(Color.BLACK)
			isFocusable = true
			isVisible = false
			addView(report)
		}
		setContentView(FrameLayout(this).apply {
			setBackgroundColor(Color.BLACK)
			addView(surfaceView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
			addView(status, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.TOP))
			addView(reportScroll, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
		})

		lifecycleScope.launch { runBenchmark() }
	}

	private suspend fun runBenchmark() {
		val benchmark = DecoderBenchmark(this)
		val results = benchmarkClips.mapIndexed { index, clip ->
			status.text = getString(R.string.ephyra_benchmark_running, index + 1, benchmarkClips.size, clip.label)
			benchmark.run(clip, surfaceView).also { Timber.i("Codec benchmark: %s", it) }
		}

		status.text = getString(R.string.ephyra_benchmark_server)
		val serverResults = runServerPass()

		val markdown = buildString {
			appendLine("# Ephyra codec benchmark")
			appendLine()
			appendLine("- App: ${BuildConfig.VERSION_NAME}")
			appendLine("- Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE}), Android ${Build.VERSION.RELEASE}")
			appendLine()
			appendLine("## Decoder pass")
			appendLine()
			appendLine(
				"Each clip streamed from repo.jellyfin.org for ${DecoderBenchmark.DEFAULT_PLAY_SECONDS} s; " +
					"rebuffers measure the internet link."
			)
			appendLine()
			append(decoderReport(results))
			appendLine()
			appendLine("## Server pass")
			appendLine()
			if (serverResults == null) appendLine("Skipped: not signed in to a server.")
			else append(serverReport(serverResults))
			appendLine()
			appendLine("## Suggested settings")
			appendLine()
			benchmarkSuggestions(results, serverResults.orEmpty()).ifEmpty { listOf("None.") }.forEach { appendLine("- $it") }
		}
		Timber.i("Codec benchmark report:\n%s", markdown)

		surfaceView.isVisible = false
		status.isVisible = false
		report.text = markdown
		reportScroll.isVisible = true
		reportScroll.requestFocus()

		upload(markdown)
	}

	/** The server's decisions for the clips found in the user's libraries, or null without a signed-in server. */
	private suspend fun runServerPass(): List<ServerResult>? {
		if (api.accessToken == null) return null
		return try {
			val profile = createDeviceProfile(this, userPreferences, get<ServerVersion>())
			ServerBenchmark(api, profile).run(benchmarkClips)
		} catch (error: CancellationException) {
			throw error
		} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
			Timber.w(error, "Codec benchmark server pass failed")
			benchmarkClips.map { ServerResult(it.label, error = error.javaClass.simpleName) }
		}
	}

	private suspend fun upload(markdown: String) {
		if (api.accessToken == null) return
		try {
			val fileName = api.clientLogApi.logFile(markdown).content.fileName
			report.append("\n${getString(R.string.ephyra_benchmark_uploaded, fileName)}")
		} catch (error: CancellationException) {
			throw error
		} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
			Timber.w(error, "Could not upload the codec benchmark report")
		}
	}

	private companion object {
		const val OVERSCAN_PADDING = 48
		const val SHADOW_RADIUS = 4f
		const val STATUS_TEXT_SIZE = 20f
		const val REPORT_TEXT_SIZE = 12f
	}
}
