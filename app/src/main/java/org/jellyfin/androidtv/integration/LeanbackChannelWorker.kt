package org.jellyfin.androidtv.integration

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.TvContractCompat.WatchNextPrograms
import androidx.tvprovider.media.tv.WatchNextProgram
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.integration.provider.ImageProvider
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.androidtv.util.dp
import org.jellyfin.androidtv.util.sdk.isUsable
import org.jellyfin.androidtv.util.stripHtml
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.TimeoutException
import org.jellyfin.sdk.api.client.extensions.userViewApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.extensions.ticks
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Manages channels on the android tv home screen.
 *
 * More info: https://developer.android.com/training/tv/discovery/recommendations-channel.
 */
class LeanbackChannelWorker(
	private val context: Context,
	workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams), KoinComponent {
	companion object {
		private const val PERIODIC_UPDATE_REQUEST_NAME = "LeanbackChannelPeriodicUpdateRequest"
		private const val CHANNEL_STORE_NAME = "leanback_channels"
		private const val BACKOFF_DELAY_MINUTES = 10L

		/**
		 * Check if the app can use Android TV home screen channels on this device.
		 */
		fun isAvailable(context: Context) = AndroidVersion.isAtLeastO &&
			context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) &&
			context.packageManager.resolveContentProvider(TvContractCompat.AUTHORITY, 0) != null

		suspend fun enqueue(workManager: WorkManager) {
			workManager.enqueueUniquePeriodicWork(
				PERIODIC_UPDATE_REQUEST_NAME,
				ExistingPeriodicWorkPolicy.UPDATE,
				PeriodicWorkRequestBuilder<LeanbackChannelWorker>(1, TimeUnit.HOURS)
					.setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
					.build()
			).await()
		}
	}

	private val api by inject<ApiClient>()
	private val userPreferences by inject<UserPreferences>()
	private val userViewsRepository by inject<UserViewsRepository>()
	private val imageHelper by inject<ImageHelper>()

	/**
	 * Update all channels for the currently authenticated user.
	 */
	override suspend fun doWork(): Result = when {
		// Succeed immediatly when not available
		!isAvailable(context) -> Result.success()
		// Clear all integration data when disabled
		!userPreferences[UserPreferences.tvProviderEnabled] -> {
			clearChannels()
			Result.success()
		}
		// Retry later if no authenticated user is found
		!api.isUsable -> Result.retry()
		else -> updateChannels()
	}

	/**
	 * Replace the programs of all channels with the current server state, retry later on server errors.
	 */
	private suspend fun updateChannels(): Result = try {
		// Get next up episodes
		val (resumeItems, nextUpItems) = getNextUpItems(api)
		// Get latest media
		val (latestEpisodes, latestMovies, latestMedia) = getLatestMedia(api)
		val myMedia = getMyMedia()
		// Delete current items from the channels
		context.contentResolver.delete(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null)

		// Get channel URIs
		val latestMediaChannel = getPreviewChannelUri("latest_media", R.string.home_section_latest_media, default = true)
		val myMediaChannel = getPreviewChannelUri("my_media", R.string.lbl_my_media)
		val nextUpChannel = getPreviewChannelUri("next_up", R.string.lbl_next_up)
		val latestMoviesChannel = getPreviewChannelUri("latest_movies", R.string.lbl_movies)
		val latestEpisodesChannel = getPreviewChannelUri("latest_episodes", R.string.lbl_new_episodes)
		val preferParentThumb = userPreferences[UserPreferences.seriesThumbnailsEnabled]

		// Add new items
		arrayOf(
			nextUpItems to nextUpChannel,
			latestMedia to latestMediaChannel,
			latestMovies to latestMoviesChannel,
			latestEpisodes to latestEpisodesChannel,
			myMedia to myMediaChannel,
		).forEach { (items, channel) ->
			if (channel == null) {
				Timber.e("Skipping channel because it was not available")
			} else {
				insertPreviewPrograms(channel, items, preferParentThumb)
			}
		}
		updateWatchNext(resumeItems + nextUpItems)

		// Success!
		Result.success()
	} catch (err: TimeoutException) {
		Timber.w(err, "Server unreachable, trying again later")

		Result.retry()
	} catch (err: ApiClientException) {
		Timber.e(err, "SDK error, trying again later")

		Result.retry()
	}

	private fun getPreviewChannelUri(name: String, @StringRes displayNameRes: Int, default: Boolean = false): Uri? = getChannelUri(
		name,
		Channel.Builder()
			.setType(TvContractCompat.Channels.TYPE_PREVIEW)
			.setDisplayName(context.getString(displayNameRes))
			.setAppLinkIntent(Intent(context, StartupActivity::class.java))
			.build(),
		default = default,
	)

	@SuppressLint("RestrictedApi")
	private fun insertPreviewPrograms(channel: Uri, items: List<BaseItemDto>, preferParentThumb: Boolean) {
		val programs = items.map { item -> createPreviewProgram(channel, item, preferParentThumb) }
		context.contentResolver.bulkInsert(TvContractCompat.PreviewPrograms.CONTENT_URI, programs.toTypedArray())
	}

	/**
	 * Remove all channels and programs managed by the Android TV home screen integration.
	 */
	private fun clearChannels() {
		context.contentResolver.delete(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null)
		context.contentResolver.delete(WatchNextPrograms.CONTENT_URI, null, null)

		val store = context.getSharedPreferences(CHANNEL_STORE_NAME, Context.MODE_PRIVATE)
		store.all.values
			.filterIsInstance<String>()
			.map(Uri::parse)
			.forEach { uri -> context.contentResolver.delete(uri, null, null) }
		store.edit { clear() }
	}

	/**
	 * Get the uri for a channel or create it if it doesn't exist. Uses the [settings] parameter to
	 * update or create the channel. The [name] parameter is used to store the id and should be
	 * unique.
	 */
	private fun getChannelUri(name: String, settings: Channel, default: Boolean = false): Uri? {
		val store = context.getSharedPreferences(CHANNEL_STORE_NAME, Context.MODE_PRIVATE)
		var uri: Uri? = null

		// Try and re-use our existing channel definition
		if (store.contains(name)) {
			uri = store.getString(name, null)?.toUri()

			if (uri != null) {
				val result = context.contentResolver.update(uri, settings.toContentValues(), null, null)
				// If we did not affect exactly 1 row there might be something wrong, so recreate it
				if (result != 1) uri = null
			}
		}

		if (uri == null) {
			// Create new channel
			uri = context.contentResolver.insert(
				TvContractCompat.Channels.CONTENT_URI,
				settings.toContentValues()
			)

			// Set as default row to display (we can request one row to automatically be added to the home screen)
			if (uri != null && default) {
				TvContractCompat.requestChannelBrowsable(context, ContentUris.parseId(uri))
			}

			// Save uri to shared preferences
			store.edit { putString(name, uri?.toString()) }
		}

		// Update logo
		if (uri != null) {
			ResourcesCompat.getDrawable(context.resources, R.mipmap.app_icon, context.theme)?.let {
				ChannelLogoUtils.storeChannelLogo(
					context,
					ContentUris.parseId(uri),
					it.toBitmap(80.dp(context), 80.dp(context))
				)
			}
		}

		return uri
	}

	/**
	 * Updates the "my media" row with current media libraries.
	 */
	@Suppress("RestrictedApi")
	private suspend fun getMyMedia(): List<BaseItemDto> {
		val response by api.userViewApi.getUserViews(includeHidden = false)

		// Add new items
		return response.items
			.filter { userViewsRepository.isSupported(it.collectionType) }
	}

	/**
	 * Gets the poster art for an item. Uses the [preferParentThumb] parameter to fetch the series
	 * image when preferred.
	 */
	private fun BaseItemDto.getPosterArtImageUrl(
		preferParentThumb: Boolean
	): Uri = when {
		type == BaseItemKind.MOVIE || type == BaseItemKind.SERIES -> itemImages[ImageType.PRIMARY]
		(preferParentThumb || !itemImages.contains(ImageType.PRIMARY)) && parentImages.contains(ImageType.THUMB) -> parentImages[ImageType.THUMB]
		else -> itemImages[ImageType.PRIMARY]
	}.let { image ->
		ImageProvider.getImageUri(image?.getUrl(api) ?: imageHelper.getResourceUrl(context, R.drawable.tile_land_tv))
	}

	@SuppressLint("RestrictedApi")
	private fun createPreviewProgram(
		channelUri: Uri,
		item: BaseItemDto,
		preferParentThumb: Boolean
	): ContentValues {
		val imageUri = item.getPosterArtImageUrl(preferParentThumb)
		val seasonString = item.parentIndexNumber?.toString().orEmpty()

		val episodeString = item.getEpisodeString()

		return PreviewProgram.Builder()
			.setChannelId(ContentUris.parseId(channelUri))
			.setType(item.getProgramType())
			.setTitle(item.seriesName ?: item.name)
			.setEpisodeTitle(if (item.type == BaseItemKind.EPISODE) item.name else null)
			.setDescription(item.overview?.stripHtml())
			.setReleaseDate(
				if (item.premiereDate != null) DateTimeFormatter.ISO_DATE.format(item.premiereDate)
				else null
			)
			.setPosterArtUri(imageUri)
			.setPosterArtAspectRatio(item.getPosterAspectRatio())
			.setIntent(Intent(context, StartupActivity::class.java).apply {
				putExtra(StartupActivity.EXTRA_ITEM_ID, item.id.toString())
				putExtra(StartupActivity.EXTRA_ITEM_IS_USER_VIEW, item.type == BaseItemKind.COLLECTION_FOLDER)
			})
			.setDurationMillis(item.getRemainingDurationMillis())
			.apply {
				if ((item.parentIndexNumber ?: 0) > 0)
					setSeasonNumber(seasonString, item.parentIndexNumber!!)
				if ((item.indexNumber ?: 0) > 0)
					setEpisodeNumber(episodeString, item.indexNumber!!)
			}.build().toContentValues()
	}

	/**
	 * Updates the "watch next" row with new and unfinished episodes. Does not include movies, music
	 * or other types of media. Uses the [nextUpItems] parameter to store items returned by a
	 * NextUpQuery().
	 */
	@SuppressLint("RestrictedApi")
	private fun updateWatchNext(nextUpItems: List<BaseItemDto>) {
		deletePrograms(nextUpItems)

		// Get current watch next state
		val currentWatchNextPrograms = getCurrentWatchNext()

		// Create all programs in nextUpItems but not in watch next
		val programsToAdd = nextUpItems
			.filter { next -> currentWatchNextPrograms.none { it.internalProviderId == next.id.toString() } }
		context.contentResolver.bulkInsert(
			WatchNextPrograms.CONTENT_URI,
			programsToAdd.map { item -> getBaseItemAsWatchNextProgram(item).toContentValues() }
				.toTypedArray())
	}

	/**
	 * Delete stale programs from the watch next row. Items that don't need to be touched are
	 * kept as is, so they keep their ordering in the watch next row.
	 */
	@SuppressLint("RestrictedApi")
	private fun deletePrograms(nextUpItems: List<BaseItemDto>) {
		// Retrieve current watch next row
		val currentWatchNextPrograms = getCurrentWatchNext()

		// Find all stale programs to delete
		val deletedByUser = currentWatchNextPrograms.filter { !it.isBrowsable }
		val noLongerInWatchNext =
			currentWatchNextPrograms.filter { (nextUpItems).none { next -> it.internalProviderId == next.id.toString() } }
		val continueWatching = currentWatchNextPrograms.filter { it.watchNextType == WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE }

		// Delete the programs
		(deletedByUser + noLongerInWatchNext + continueWatching)
			.forEach { context.contentResolver.delete(TvContractCompat.buildWatchNextProgramUri(it.id), null, null) }
	}

	/**
	 * Retrieves the current watch next row state.
	 */
	@SuppressLint("RestrictedApi")
	private fun getCurrentWatchNext(): MutableList<WatchNextProgram> {
		val currentWatchNextPrograms: MutableList<WatchNextProgram> = mutableListOf()
		context.contentResolver.query(WatchNextPrograms.CONTENT_URI, WatchNextProgram.PROJECTION, null, null, null)
			.use { cursor ->
				if (cursor != null && cursor.moveToFirst()) {
					do {
						currentWatchNextPrograms.add(WatchNextProgram.fromCursor(cursor))
					} while (cursor.moveToNext())
				}
			}
		return currentWatchNextPrograms
	}

	/**
	 * Convert [BaseItemDto] to [WatchNextProgram]. Assumes the item type is "episode".
	 */
	@Suppress("RestrictedApi")
	private fun getBaseItemAsWatchNextProgram(item: BaseItemDto) =
		WatchNextProgram.Builder().apply {
			val preferParentThumb = userPreferences[UserPreferences.seriesThumbnailsEnabled]

			setInternalProviderId(item.id.toString())

			setTypeAndPosterRatio(item)

			// Name and episode details
			if (item.seriesName != null) {
				setTitle(item.seriesName)
				setEpisodeTitle(item.name)

				item.indexNumber?.takeIf { it > 0 }?.let { setEpisodeNumber(it) }
				item.parentIndexNumber?.takeIf { it > 0 }?.let { setSeasonNumber(it) }
			} else {
				setTitle(item.name)
			}

			setDescription(item.overview?.stripHtml())

			// Poster
			setPosterArtUri(item.getPosterArtImageUrl(preferParentThumb))

			setWatchNextState(item)

			// Runtime has been determined
			item.runTimeTicks?.ticks?.let { setDurationMillis(it.inWholeMilliseconds.toInt()) }

			// Set intent to open the episode
			setIntent(Intent(context, StartupActivity::class.java).apply {
				putExtra(StartupActivity.EXTRA_ITEM_ID, item.id.toString())
			})
		}.build()
}
