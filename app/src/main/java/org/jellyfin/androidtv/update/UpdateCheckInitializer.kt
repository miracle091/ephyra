package org.jellyfin.androidtv.update

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.di.KoinInitializer

/** Ephyra: starts [DailyUpdateCheck] in release builds; debug builds have their own application id. */
@Suppress("unused")
class UpdateCheckInitializer : Initializer<Unit> {
	override fun create(context: Context) {
		if (BuildConfig.DEBUG) return

		val koin = AppInitializer.getInstance(context)
			.initializeComponent(KoinInitializer::class.java)
			.koin

		ProcessLifecycleOwner.get().lifecycle.addObserver(koin.get<DailyUpdateCheck>())
	}

	override fun dependencies() = listOf(KoinInitializer::class.java)
}
