package org.jellyfin.androidtv.update

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class AppUpdateTest : FunSpec({
	test("Parses the version file written by the versionTxt task") {
		AppUpdate.parseVersionTxt("v1.2.3=1020399\n") shouldBe AppUpdate("1.2.3", 1020399)
		AppUpdate.parseVersionTxt("v2.0.0-rc.3=2000003") shouldBe AppUpdate("2.0.0-rc.3", 2000003)
	}

	test("Rejects malformed version files") {
		AppUpdate.parseVersionTxt("").shouldBeNull()
		AppUpdate.parseVersionTxt("<html>Not Found</html>").shouldBeNull()
		AppUpdate.parseVersionTxt("v1.2.3").shouldBeNull()
		AppUpdate.parseVersionTxt("v1.2.3=abc").shouldBeNull()
		AppUpdate.parseVersionTxt("v1.2.3=-5").shouldBeNull()
		AppUpdate.parseVersionTxt("v1.2.3=1=2").shouldBeNull()
	}

	test("Rejects version names that would change the download URL") {
		AppUpdate.parseVersionTxt("v1.2.3/../../evil=1020399").shouldBeNull()
		AppUpdate.parseVersionTxt("v1.2.3?x=1020399").shouldBeNull()
		AppUpdate.parseVersionTxt("v1.2=1020399").shouldBeNull()
	}

	test("Points at the APK of the release's own tag") {
		AppUpdate("1.2.3", 1020399).apkUrl shouldBe
			"https://github.com/miracle091/ephyra/releases/download/v1.2.3/ephyra-v1.2.3.apk"
		AppUpdate.VERSION_TXT_URL shouldBe "https://github.com/miracle091/ephyra/releases/latest/download/version.txt"
	}
})
