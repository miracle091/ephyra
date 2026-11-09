package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod

class StreamRulesTest : FunSpec({
	test("a subtitle the server burns in is the one on screen, whatever the app picked") {
		StreamRules.shownSubtitleIndex(SubtitleDeliveryMethod.ENCODE, serverIndex = 3, pickedIndex = null) shouldBe 3
		StreamRules.shownSubtitleIndex(SubtitleDeliveryMethod.ENCODE, serverIndex = 3, pickedIndex = 5) shouldBe 3
	}

	test("without burn-in the app's pick stays") {
		StreamRules.shownSubtitleIndex(SubtitleDeliveryMethod.EMBED, serverIndex = 3, pickedIndex = null) shouldBe null
		StreamRules.shownSubtitleIndex(SubtitleDeliveryMethod.EXTERNAL, serverIndex = 3, pickedIndex = 4) shouldBe 4
		StreamRules.shownSubtitleIndex(SubtitleDeliveryMethod.DROP, serverIndex = -1, pickedIndex = null) shouldBe null
	}

	test("burn-in without a real subtitle index keeps the app's pick") {
		StreamRules.shownSubtitleIndex(SubtitleDeliveryMethod.ENCODE, serverIndex = null, pickedIndex = 2) shouldBe 2
		StreamRules.shownSubtitleIndex(SubtitleDeliveryMethod.ENCODE, serverIndex = -1, pickedIndex = null) shouldBe null
	}
})
