package org.jellyfin.androidtv.update

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.data.model.AppNotification
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.preference.UpdatePreferences
import org.jellyfin.preference.Preference
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val NOW = 100.days.inWholeMilliseconds
private val UPDATE = AppUpdate("9.0.0", 9000099)

private class Fixture(checkResult: AppUpdateState, vararg stored: Pair<Preference<*>, Any>) {
	private val sharedPreferences = MapSharedPreferences().apply {
		stored.forEach { (preference, value) -> values[preference.key] = value }
	}
	val values get() = sharedPreferences.values
	val preferences = UpdatePreferences(mockk<Context> { every { getSharedPreferences(any(), any()) } returns sharedPreferences })
	val notifications = MutableStateFlow(emptyList<AppNotification>())
	val dismiss = slot<() -> Unit>()
	val notificationsRepository = mockk<NotificationsRepository> {
		every { this@mockk.notifications } returns this@Fixture.notifications
		every { addNotification(any(), any(), capture(dismiss)) } answers {
			AppNotification(firstArg(), thirdArg(), secondArg()).also { this@Fixture.notifications.value += it }
		}
	}
	val appUpdater = mockk<AppUpdater> { coEvery { check(any()) } returns checkResult }
	val context = mockk<Context> {
		every { getString(any(), *anyVararg()) } answers { "Ephyra ${(args[1] as Array<*>)[0]} is available" }
	}
	val dailyCheck = DailyUpdateCheck(context, appUpdater, preferences, notificationsRepository) { NOW }
}

class DailyUpdateCheckTest : FunSpec({
	test("Checks when the last check is a day old and shows a notice for a newer version") {
		val fixture = Fixture(AppUpdateState.Available(UPDATE), UpdatePreferences.lastCheck to NOW - 1.days.inWholeMilliseconds)
		fixture.dailyCheck.checkIfDue()

		fixture.notifications.value.map { it.message } shouldBe listOf("Ephyra 9.0.0 is available")
		fixture.values[UpdatePreferences.lastCheck.key] shouldBe NOW
	}

	test("Skips the check within a day of the last one") {
		val fixture = Fixture(AppUpdateState.Available(UPDATE), UpdatePreferences.lastCheck to NOW - 23.hours.inWholeMilliseconds)
		fixture.dailyCheck.checkIfDue()

		coVerify(exactly = 0) { fixture.appUpdater.check(any()) }
	}

	test("Skips the check when the setting is off") {
		val fixture = Fixture(AppUpdateState.Available(UPDATE), UpdatePreferences.dailyCheckEnabled to false)
		fixture.dailyCheck.checkIfDue()

		coVerify(exactly = 0) { fixture.appUpdater.check(any()) }
	}

	test("A failed check is retried at the next start") {
		val fixture = Fixture(AppUpdateState.Failed)
		fixture.dailyCheck.checkIfDue()

		fixture.values[UpdatePreferences.lastCheck.key] shouldBe null
		fixture.notifications.value shouldBe emptyList()
	}

	test("A dismissed notice is not shown again for the same version") {
		val fixture = Fixture(AppUpdateState.Available(UPDATE))
		fixture.dailyCheck.checkIfDue()
		fixture.dismiss.captured()
		fixture.values[UpdatePreferences.dismissedVersion.key] shouldBe "9.0.0"

		fixture.notifications.value = emptyList()
		fixture.values.remove(UpdatePreferences.lastCheck.key)
		fixture.dailyCheck.checkIfDue()

		fixture.notifications.value shouldBe emptyList()
	}

	test("The same notice is not added twice") {
		val fixture = Fixture(AppUpdateState.Available(UPDATE))
		fixture.dailyCheck.checkIfDue()
		fixture.values.remove(UpdatePreferences.lastCheck.key)
		fixture.dailyCheck.checkIfDue()

		verify(exactly = 1) { fixture.notificationsRepository.addNotification(any(), any(), any()) }
	}
})
