package org.jellyfin.androidtv.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/** Ephyra: receives the result of an [AppUpdater] installation from PackageInstaller. */
class AppUpdateStatusReceiver : BroadcastReceiver(), KoinComponent {
	private val appUpdater by inject<AppUpdater>()

	override fun onReceive(context: Context, intent: Intent) {
		when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
			// The system confirmation dialog; the user started the update from the settings, so the app is in the foreground
			PackageInstaller.STATUS_PENDING_USER_ACTION -> {
				val confirmIntent = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
				if (confirmIntent != null) context.startActivity(confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
				else appUpdater.onInstallFailed()
			}

			PackageInstaller.STATUS_SUCCESS -> Timber.i("Update installed")

			else -> {
				Timber.w("Update failed ($status): ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
				appUpdater.onInstallFailed()
			}
		}
	}
}
