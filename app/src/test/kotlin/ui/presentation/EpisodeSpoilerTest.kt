package org.jellyfin.androidtv.ui.presentation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.JellyfinImageSource
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.util.UUID

class EpisodeSpoilerTest : FunSpec({
	val episodeId = UUID.randomUUID()
	val seriesId = UUID.randomUUID()

	fun userData(played: Boolean = false, position: Long = 0) = UserItemDataDto(
		played = played,
		playbackPositionTicks = position,
		playCount = 0,
		isFavorite = false,
		key = "",
		itemId = episodeId,
	)

	fun episode(userData: UserItemDataDto?, type: BaseItemKind = BaseItemKind.EPISODE) =
		BaseItemDto(id = episodeId, type = type, seriesId = seriesId, userData = userData)

	fun image(item: UUID, source: JellyfinImageSource = JellyfinImageSource.ITEM) =
		JellyfinImage(item = item, source = source, type = ImageType.PRIMARY, tag = "t", blurHash = "h", aspectRatio = 1.78f, index = null)

	test("the own picture of an episode not started yet is a spoiler") {
		isEpisodeSpoiler(episode(userData()), image(episodeId)) shouldBe true
		isEpisodeSpoiler(episode(null), image(episodeId)) shouldBe true
	}

	test("watched or started episodes, series pictures and other items are not") {
		isEpisodeSpoiler(episode(userData(played = true)), image(episodeId)) shouldBe false
		isEpisodeSpoiler(episode(userData(position = 1)), image(episodeId)) shouldBe false
		isEpisodeSpoiler(episode(userData()), image(seriesId, JellyfinImageSource.SERIES)) shouldBe false
		isEpisodeSpoiler(episode(userData(), BaseItemKind.MOVIE), image(episodeId)) shouldBe false
		isEpisodeSpoiler(null, image(episodeId)) shouldBe false
	}
})
