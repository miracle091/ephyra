package org.jellyfin.androidtv.ui.player.video

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.preference.constant.StillWatchingBehavior
import org.jellyfin.sdk.model.api.BaseItemKind

class EndOfEntryPromptTest : FunSpec({
	test("next up is offered after an episode when it is on") {
		endOfEntryPrompt(BaseItemKind.EPISODE, NextUpBehavior.EXTENDED, StillWatchingBehavior.DISABLED, stillWatchingDue = false) shouldBe
			EndOfEntryPrompt.NEXT_UP
	}

	test("still watching comes first once it is due") {
		endOfEntryPrompt(BaseItemKind.EPISODE, NextUpBehavior.EXTENDED, StillWatchingBehavior.DEFAULT, stillWatchingDue = true) shouldBe
			EndOfEntryPrompt.STILL_WATCHING
	}

	test("before still watching is due, next up is offered") {
		endOfEntryPrompt(BaseItemKind.EPISODE, NextUpBehavior.MINIMAL, StillWatchingBehavior.DEFAULT, stillWatchingDue = false) shouldBe
			EndOfEntryPrompt.NEXT_UP
	}

	test("with next up off and still watching not due, the queue moves on by itself") {
		endOfEntryPrompt(BaseItemKind.EPISODE, NextUpBehavior.DISABLED, StillWatchingBehavior.DEFAULT, stillWatchingDue = false) shouldBe
			EndOfEntryPrompt.NONE
	}

	test("trailers never prompt") {
		endOfEntryPrompt(BaseItemKind.TRAILER, NextUpBehavior.EXTENDED, StillWatchingBehavior.DEFAULT, stillWatchingDue = true) shouldBe
			EndOfEntryPrompt.NONE
	}

	test("the controls show while no prompt is asked") {
		playerOverlay(EndOfEntryPrompt.NONE, holding = false) shouldBe PlayerOverlay.CONTROLS
	}

	test("the prompt shows while the queue holds") {
		playerOverlay(EndOfEntryPrompt.NEXT_UP, holding = true) shouldBe PlayerOverlay.END_OF_ENTRY
	}

	test("the controls stay away when the hold ends without a new entry, as when the player closes") {
		playerOverlay(EndOfEntryPrompt.NEXT_UP, holding = false) shouldBe PlayerOverlay.NONE
	}
})
