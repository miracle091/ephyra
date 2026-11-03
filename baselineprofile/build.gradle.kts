// Ephyra: generates the app's baseline profile on a connected device, see StartupBaselineProfile
plugins {
	alias(libs.plugins.android.test)
	alias(libs.plugins.androidx.baselineprofile)
}

android {
	namespace = "org.jellyfin.androidtv.baselineprofile"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	targetProjectPath = ":app"

	lint {
		lintConfig = file("$rootDir/android-lint.xml")
		abortOnError = false
	}
}

baselineProfile {
	// The Shield or a TV emulator connected through adb; no Gradle managed device
	useConnectedDevices = true
}

dependencies {
	implementation(libs.androidx.benchmark.macro.junit4)
	implementation(libs.androidx.test.junit)
}
