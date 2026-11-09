package org.jellyfin.androidtv.ui.livetv

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class GuideStartHelperTest : FunSpec({
	val pageSize = 20
	val channel = UUID.randomUUID()

	test("a last channel past the first page is centred in its page") {
		guideStartIndex(lastChannelIndex = 45, pageSize = pageSize, restoreLastChannel = true) shouldBe 35
	}

	test("a last channel in the first page starts at the top") {
		guideStartIndex(lastChannelIndex = 5, pageSize = pageSize, restoreLastChannel = true) shouldBe 0
	}

	test("with restoring off the guide starts at the top whatever the last channel") {
		guideStartIndex(lastChannelIndex = 45, pageSize = pageSize, restoreLastChannel = false) shouldBe 0
	}

	test("the last channel is focused only when restoring") {
		guideFocusChannelId(channel, restoreLastChannel = true) shouldBe channel
		guideFocusChannelId(channel, restoreLastChannel = false) shouldBe null
		guideFocusChannelId(null, restoreLastChannel = true) shouldBe null
	}
})
