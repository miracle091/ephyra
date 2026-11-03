package org.jellyfin.androidtv.update

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.preference.UpdatePreferences
import kotlin.time.Duration.Companion.days

/**
 * Ephyra: each time the app comes to the foreground, checks GitHub Releases if the last check is a day old, and shows
 * a notice on the home screen when a newer version exists. Installing stays a choice in Settings, About.
 */
class DailyUpdateCheck(
	private val context: Context,
	private val appUpdater: AppUpdater,
	private val updatePreferences: UpdatePreferences,
	private val notificationsRepository: NotificationsRepository,
	private val now: () -> Long = System::currentTimeMillis,
) : DefaultLifecycleObserver {
	override fun onStart(owner: LifecycleOwner) {
		owner.lifecycleScope.launch { checkIfDue() }
	}

	suspend fun checkIfDue() {
		if (!updatePreferences[UpdatePreferences.dailyCheckEnabled]) return
		val lastCheck = updatePreferences[UpdatePreferences.lastCheck]
		if (now() - lastCheck in 0 until CHECK_INTERVAL_MS) return

		when (val state = appUpdater.check(background = true)) {
			is AppUpdateState.Available -> {
				updatePreferences[UpdatePreferences.lastCheck] = now()
				notify(state.update)
			}

			AppUpdateState.UpToDate -> updatePreferences[UpdatePreferences.lastCheck] = now()

			// A failed check is retried at the next start; a running one is the user's
			AppUpdateState.Failed, AppUpdateState.Idle, AppUpdateState.Checking, is AppUpdateState.Installing -> Unit
		}
	}

	private fun notify(update: AppUpdate) {
		if (updatePreferences[UpdatePreferences.dismissedVersion] == update.versionName) return

		val message = context.getString(R.string.ephyra_update_notification, update.versionName)
		if (notificationsRepository.notifications.value.any { it.message == message }) return

		notificationsRepository.addNotification(message) {
			updatePreferences[UpdatePreferences.dismissedVersion] = update.versionName
		}
	}

	private companion object {
		val CHECK_INTERVAL_MS = 1.days.inWholeMilliseconds
	}
}
