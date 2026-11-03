package org.jellyfin.androidtv.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code the app runs at startup and while moving through the first screen with the D-pad, so ART compiles
 * it ahead of time even for sideloaded APKs (profileinstaller). Run with `./gradlew :app:generateBaselineProfile`
 * on a device with root, or on Android 13 or later. On a fresh install the first screen is the server selection;
 * the home rows are covered only when the device is already signed in to a server.
 */
@RunWith(AndroidJUnit4::class)
class StartupBaselineProfile {
	@get:Rule
	val rule = BaselineProfileRule()

	@Test
	fun generate() = rule.collect(
		// The release application id: the profile is recorded on the non-minified release build, which has no suffix
		packageName = TARGET_APP_ID,
	) {
		pressHome()
		startActivityAndWait()

		// Move through rows and cards: on the home screen this is where Leanback inflation cost shows up
		repeat(DPAD_ROWS) {
			device.pressDPadDown()
			device.waitForIdle()
		}
		repeat(DPAD_CARDS) {
			device.pressDPadRight()
			device.waitForIdle()
		}
	}

	private companion object {
		const val TARGET_APP_ID = "com.ephyra.app"
		const val DPAD_ROWS = 6
		const val DPAD_CARDS = 10
	}
}
