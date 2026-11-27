package org.jellyfin.androidtv.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.AppNotification
import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.androidtv.util.isTvDevice

interface NotificationsRepository {
	val notifications: StateFlow<List<AppNotification>>

	fun dismissNotification(item: AppNotification)
	fun addDefaultNotifications()
	// Velella: no "update your server soon" notice: the SDK accepts only servers 12.0+, above the old 10.11 threshold

	// Velella: lets the update check (update/DailyUpdateCheck.kt) add its notice
	fun addNotification(message: String, public: Boolean = false, dismiss: () -> Unit = {}): AppNotification
}

class NotificationsRepositoryImpl(
	private val context: Context,
	private val systemPreferences: SystemPreferences,
) : NotificationsRepository {
	override val notifications = MutableStateFlow(emptyList<AppNotification>())

	init {
		addDefaultNotifications()
	}

	override fun dismissNotification(item: AppNotification) {
		notifications.value = notifications.value.filter { it != item }
		item.dismiss()
	}

	override fun addDefaultNotifications() {
		addUiModeNotification()
		addBetaNotification()
	}

	override fun addNotification(
		message: String,
		public: Boolean,
		dismiss: () -> Unit
	): AppNotification {
		val notification = AppNotification(message, dismiss, public)
		notifications.value += notification
		return notification
	}

	private fun addUiModeNotification() {
		val disableUiModeWarning = systemPreferences[SystemPreferences.disableUiModeWarning]

		if (!context.isTvDevice() && !disableUiModeWarning) {
			addNotification(
				context.getString(R.string.app_notification_uimode_invalid),
				public = true
			)
		}
	}

	private fun addBetaNotification() {
		val dismissedVersion = systemPreferences[SystemPreferences.dismissedBetaNotificationVersion]
		val currentVersion = BuildConfig.VERSION_NAME
		val isBeta = currentVersion.lowercase().contains("beta")

		if (isBeta && currentVersion != dismissedVersion) {
			addNotification(context.getString(R.string.app_notification_beta, currentVersion)) {
				systemPreferences[SystemPreferences.dismissedBetaNotificationVersion] =
					currentVersion
			}
		}
	}
}
