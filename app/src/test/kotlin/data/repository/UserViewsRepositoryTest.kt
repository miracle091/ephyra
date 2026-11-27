package org.jellyfin.androidtv.data.repository

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.sdk.model.api.CollectionType

class UserViewsRepositoryTest : FunSpec({
	fun repository(showPlaylists: Boolean) = UserViewsRepositoryImpl(
		api = mockk(relaxed = true),
		velellaPreferences = mockk { every { this@mockk[VelellaPreferences.homePlaylists] } returns showPlaylists },
	)

	test("playlists are left out of the home's libraries by default") {
		VelellaPreferences.homePlaylists.defaultValue shouldBe false
		repository(showPlaylists = false).isShownInMyMedia(CollectionType.PLAYLISTS) shouldBe false
	}

	test("playlists are shown among the home's libraries when the setting is on") {
		repository(showPlaylists = true).isShownInMyMedia(CollectionType.PLAYLISTS) shouldBe true
	}

	test("films, shows, music and live TV are shown among the home's libraries") {
		val repository = repository(showPlaylists = false)
		repository.isShownInMyMedia(CollectionType.MOVIES) shouldBe true
		repository.isShownInMyMedia(CollectionType.TVSHOWS) shouldBe true
		repository.isShownInMyMedia(CollectionType.MUSIC) shouldBe true
		repository.isShownInMyMedia(CollectionType.LIVETV) shouldBe true
	}

	test("unsupported libraries stay hidden") {
		repository(showPlaylists = true).isShownInMyMedia(CollectionType.BOOKS) shouldBe false
	}
})
