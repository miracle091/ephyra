package org.jellyfin.androidtv.integration

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import org.jellyfin.androidtv.di.KoinInitializer

/** Ephyra: adds the [ShieldDisplayHint] notice at app start, before the home screen reads the notices. */
@Suppress("unused")
class ShieldDisplayHintInitializer : Initializer<Unit> {
	override fun create(context: Context) {
		AppInitializer.getInstance(context)
			.initializeComponent(KoinInitializer::class.java)
			.koin
			.get<ShieldDisplayHint>()
			.showIfNeeded()
	}

	override fun dependencies() = listOf(KoinInitializer::class.java)
}
