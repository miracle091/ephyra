package org.jellyfin.androidtv.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jellyfin.androidtv.BuildConfig
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration

sealed interface AppUpdateState {
	data object Idle : AppUpdateState
	data object Checking : AppUpdateState
	data object UpToDate : AppUpdateState
	data class Available(val update: AppUpdate) : AppUpdateState
	data class Installing(val update: AppUpdate) : AppUpdateState
	data object Failed : AppUpdateState
}

/**
 * Ephyra: updates the app from GitHub Releases, for sideloaded installs without a store. Android installs the update
 * only when it is signed with the same key as the installed app, and asks the user to confirm.
 */
class AppUpdater(
	private val context: Context,
	private val httpClient: OkHttpClient,
	private val scope: CoroutineScope,
) {
	private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
	val state: StateFlow<AppUpdateState> = _state.asStateFlow()

	fun checkForUpdate() {
		scope.launch { check() }
	}

	/**
	 * Checks GitHub and returns the new state. While a check or an installation runs, returns the current state.
	 * A failed [background] check returns Failed but leaves the visible state as it was: the user did not ask for it.
	 */
	suspend fun check(background: Boolean = false): AppUpdateState {
		val current = _state.value
		if (current == AppUpdateState.Checking || current is AppUpdateState.Installing) return current
		_state.value = AppUpdateState.Checking

		val result = try {
			val latest = withContext(Dispatchers.IO) { fetchLatest() }
			when {
				latest == null -> AppUpdateState.Failed
				latest.versionCode > BuildConfig.VERSION_CODE -> AppUpdateState.Available(latest)
				else -> AppUpdateState.UpToDate
			}
		} catch (err: CancellationException) {
			_state.value = current
			throw err
		} catch (@Suppress("TooGenericExceptionCaught") err: Exception) {
			Timber.w(err, "Update check failed")
			AppUpdateState.Failed
		}
		_state.value = if (background && result == AppUpdateState.Failed) current else result
		return result
	}

	fun install(update: AppUpdate) {
		if (_state.value is AppUpdateState.Installing) return
		_state.value = AppUpdateState.Installing(update)

		scope.launch {
			try {
				withContext(Dispatchers.IO) { downloadAndCommit(update) }
				// The system dialog takes over; pressing Home there reports nothing, so a click must be able to start again
				_state.value = AppUpdateState.Available(update)
			} catch (err: CancellationException) {
				throw err
			} catch (@Suppress("TooGenericExceptionCaught") err: Exception) {
				Timber.w(err, "Update download failed")
				_state.value = AppUpdateState.Failed
			}
		}
	}

	/** Called by [AppUpdateStatusReceiver] when the installation ended without replacing the app. */
	fun onInstallFailed() {
		_state.value = AppUpdateState.Failed
	}

	private fun fetchLatest(): AppUpdate? {
		val request = Request.Builder().url(AppUpdate.VERSION_TXT_URL).build()
		return httpClient.newCall(request).execute().use { response ->
			val body = response.body
			if (!response.isSuccessful || body == null || !response.request.url.isHttps) return@use null

			// Read at most a few bytes: the file is one short line, anything longer is not a version file
			val source = body.source()
			source.request(MAX_VERSION_TXT_BYTES)
			AppUpdate.parseVersionTxt(source.buffer.readUtf8(minOf(source.buffer.size, MAX_VERSION_TXT_BYTES)))
		}
	}

	private fun InputStream.copyAtMost(out: OutputStream, limit: Long) = use { input ->
		val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
		var copied = 0L
		while (true) {
			val read = input.read(buffer)
			if (read < 0) break
			copied += read
			check(copied <= limit) { "APK download failed: too large" }
			out.write(buffer, 0, read)
		}
	}

	/** Streams the APK straight into a PackageInstaller session, without a temporary file, then commits it. */
	private fun downloadAndCommit(update: AppUpdate) {
		val installer = context.packageManager.packageInstaller
		// Drop sessions left by an earlier attempt (process killed mid-download, confirmation never answered)
		installer.mySessions.forEach { installer.abandonSession(it.sessionId) }
		val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
			setAppPackageName(context.packageName)
		}
		val sessionId = installer.createSession(params)

		try {
			installer.openSession(sessionId).use { session ->
				downloadInto(session, update)

				// The system fills in the status extras, so the intent must be mutable
				val statusIntent = PendingIntent.getBroadcast(
					context,
					sessionId,
					Intent(context, AppUpdateStatusReceiver::class.java).setPackage(context.packageName),
					PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
				)
				session.commit(statusIntent.intentSender)
			}
		} catch (@Suppress("TooGenericExceptionCaught") err: Exception) {
			installer.abandonSession(sessionId)
			throw err
		}
	}

	private fun downloadInto(session: PackageInstaller.Session, update: AppUpdate) {
		val request = Request.Builder().url(update.apkUrl).build()
		httpClient.newCall(request).execute().use { response ->
			check(response.isSuccessful) { "APK download failed: HTTP ${response.code}" }
			check(response.request.url.isHttps) { "APK download failed: redirected off HTTPS" }
			val body = checkNotNull(response.body) { "APK download failed: empty response" }
			check(body.contentLength() <= MAX_APK_BYTES) { "APK download failed: too large" }
			session.openWrite("base.apk", 0, body.contentLength()).use { out ->
				body.byteStream().copyAtMost(out, MAX_APK_BYTES)
				session.fsync(out)
			}
		}
	}

	companion object {
		private const val MAX_VERSION_TXT_BYTES = 64L
		private const val MAX_APK_BYTES = 200L * 1024 * 1024

		/**
		 * The updater's client, derived from the SDK's: no limit on the whole call, which would cut a slow APK download
		 * (the read timeout still catches a stalled one), and no redirect from HTTPS to HTTP.
		 */
		fun httpClient(base: OkHttpClient): OkHttpClient = base.newBuilder()
			.callTimeout(Duration.ZERO)
			.followSslRedirects(false)
			.build()
	}
}
