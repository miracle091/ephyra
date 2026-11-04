package org.jellyfin.androidtv.integration

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.jellyfin.androidtv.data.model.AppNotification
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.preference.HintPreferences
import org.jellyfin.androidtv.update.MapSharedPreferences

private class Fixture(isShieldTv: Boolean, hdr: Boolean, dismissed: Boolean = false) {
	val sharedPreferences = MapSharedPreferences().apply {
		if (dismissed) values[HintPreferences.shieldDisplayHintDismissed.key] = true
	}
	val dismiss = slot<() -> Unit>()
	val notificationsRepository = mockk<NotificationsRepository> {
		every { addNotification(any(), any(), capture(dismiss)) } answers { AppNotification(firstArg(), thirdArg(), secondArg()) }
	}
	val context = mockk<Context> {
		every { getSharedPreferences(any(), any()) } returns sharedPreferences
		every { getString(any()) } returns "hint"
	}
	val hint = ShieldDisplayHint(
		context = context,
		hintPreferences = HintPreferences(context),
		notificationsRepository = notificationsRepository,
		isShieldTv = { isShieldTv },
		displaySupportsHdr = { hdr },
	)
}

class ShieldDisplayHintTest : FunSpec({
	test("Shows the hint on a Shield TV with an HDR display") {
		val fixture = Fixture(isShieldTv = true, hdr = true)
		fixture.hint.showIfNeeded()
		verify(exactly = 1) { fixture.notificationsRepository.addNotification("hint", any(), any()) }
	}

	test("Dismissing the hint keeps it hidden afterwards") {
		val fixture = Fixture(isShieldTv = true, hdr = true)
		fixture.hint.showIfNeeded()
		fixture.dismiss.captured()
		fixture.sharedPreferences.values[HintPreferences.shieldDisplayHintDismissed.key] shouldBe true

		fixture.hint.showIfNeeded()
		verify(exactly = 1) { fixture.notificationsRepository.addNotification(any(), any(), any()) }
	}

	test("No hint once dismissed, on other devices or without HDR") {
		listOf(
			Fixture(isShieldTv = true, hdr = true, dismissed = true),
			Fixture(isShieldTv = false, hdr = true),
			Fixture(isShieldTv = true, hdr = false),
		).forEach { fixture ->
			fixture.hint.showIfNeeded()
			verify(exactly = 0) { fixture.notificationsRepository.addNotification(any(), any(), any()) }
		}
	}
})
