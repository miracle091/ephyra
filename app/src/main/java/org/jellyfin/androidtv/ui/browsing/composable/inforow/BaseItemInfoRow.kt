package org.jellyfin.androidtv.ui.browsing.composable.inforow

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.VelellaTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.InfoRowGroups
import org.jellyfin.androidtv.ui.composable.InfoRowOutlinedBadge
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.sdk.getSeasonEpisodeName
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.SeriesStatus
import org.jellyfin.sdk.model.extensions.ticks
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.time.Duration

@Composable
fun InfoRowDate(
	item: BaseItemDto,
) {
	val date = when (item.type) {
		BaseItemKind.PERSON -> {
			val birthDate = item.premiereDate

			if (birthDate != null) {
				val age = ChronoUnit.YEARS.between(birthDate, item.endDate ?: LocalDateTime.now())
				val birthDateStr = birthDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
				stringResource(R.string.person_birthday_and_age, birthDateStr, age)
			} else null
		}

		BaseItemKind.PROGRAM, BaseItemKind.TV_CHANNEL -> when {
			item.startDate != null && item.endDate != null -> buildString {
				// Format: 20:00 - 21:30
				append(item.startDate?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
				append(" - ")
				append(item.endDate?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
			}

			else -> null
		}

		BaseItemKind.SERIES -> item.productionYear?.toString()

		else -> when {
			item.premiereDate != null -> item.premiereDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
			item.productionYear != null -> item.productionYear?.toString()
			else -> null
		}
	}

	if (date != null) {
		InfoRowItem(contentDescription = null) {
			Text(date)
		}
	}
}

@Composable
fun InfoRowSeriesStatus(
	item: BaseItemDto,
) {
	val seriesStatus = item.status?.let(SeriesStatus::fromNameOrNull)

	if (seriesStatus != null) {
		when (seriesStatus) {
			SeriesStatus.CONTINUING -> InfoRowItem(
				contentDescription = stringResource(R.string.lbl__continuing),
				colors = InfoRowColors.Green,
			) {
				Text(stringResource(R.string.lbl__continuing))
			}

			SeriesStatus.ENDED -> InfoRowItem(
				contentDescription = stringResource(R.string.lbl_ended),
				colors = InfoRowColors.Red,
			) {
				Text(stringResource(R.string.lbl_ended))
			}

			SeriesStatus.UNRELEASED -> InfoRowItem(
				contentDescription = stringResource(R.string.unreleased),
				colors = InfoRowColors.Default,
			) {
				Text(stringResource(R.string.unreleased))
			}
		}
	}
}

@Composable
fun BaseItemInfoRowRuntime(
	runTime: Duration,
) {
	InfoRowItem(
		icon = ImageVector.vectorResource(id = R.drawable.ic_time),
		contentDescription = null,
	) {
		Text(TimeUtils.formatMillis(runTime.inWholeMilliseconds))
	}
}

@Composable
fun InfoRowSeasonEpisode(item: BaseItemDto) {
	val context = LocalContext.current
	val displayName = item.getSeasonEpisodeName(context)

	if (displayName.isNotBlank()) {
		InfoRowItem(contentDescription = null) {
			Text(displayName)
		}
	}
}

@Composable
fun BaseItemInfoRow(
	item: BaseItemDto,
	mediaSource: MediaSourceInfo?,
	includeRuntime: Boolean,
	separated: Boolean = false,
) {
	// Velella: the details screen shows films, series and episodes as groups divided by vertical lines
	if (separated && item.type !in SEPARATED_EXCLUDED_KINDS) {
		InfoRowGroups(buildSeparatedGroups(item, mediaSource, includeRuntime))
		return
	}

	Row(
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		item.communityRating?.let { InfoRowCommunityRating(it / COMMUNITY_RATING_SCALE) }
		item.criticRating?.let { InfoRowCriticRating(it / CRITIC_RATING_SCALE) }

		InfoRowByKind(item, mediaSource, includeRuntime)
	}
}

/** The kind specific part of the info row, after the ratings. */
@Composable
private fun InfoRowByKind(item: BaseItemDto, mediaSource: MediaSourceInfo?, includeRuntime: Boolean) {
	when (item.type) {
		BaseItemKind.EPISODE -> {
			InfoRowSeasonEpisode(item)
			InfoRowDate(item)
			InfoRowRuntimeRatingMedia(item, mediaSource, includeRuntime)
		}

		BaseItemKind.BOX_SET -> InfoRowBoxSet(item, mediaSource, includeRuntime)
		BaseItemKind.SERIES -> InfoRowSeries(item, mediaSource, includeRuntime)
		BaseItemKind.PROGRAM -> InfoRowProgram(item, mediaSource, includeRuntime)
		BaseItemKind.MUSIC_ARTIST -> InfoRowMusicArtist(item)
		BaseItemKind.MUSIC_ALBUM -> InfoRowMusicAlbum(item)
		BaseItemKind.PLAYLIST -> InfoRowPlaylist(item, mediaSource, includeRuntime)

		else -> {
			InfoRowDate(item)
			InfoRowRuntimeRatingMedia(item, mediaSource, includeRuntime)
		}
	}
}

@Composable
private fun InfoRowSeries(item: BaseItemDto, mediaSource: MediaSourceInfo?, includeRuntime: Boolean) {
	InfoRowDate(item)
	if (includeRuntime) item.runTimeTicks?.ticks?.let { BaseItemInfoRowRuntime(it) }
	InfoRowSeriesStatus(item)
	item.officialRating?.let { InfoRowParentalRating(it) }
	mediaSource?.let { InfoRowMediaDetails(it) }
}

// Velella: the server rates out of 10 (community) and 100 (critics); the badges take a fraction
internal const val COMMUNITY_RATING_SCALE = 10f
internal const val CRITIC_RATING_SCALE = 100f

private val SEPARATED_EXCLUDED_KINDS = setOf(
	BaseItemKind.PERSON,
	BaseItemKind.PROGRAM,
	BaseItemKind.TV_CHANNEL,
	BaseItemKind.BOX_SET,
	BaseItemKind.MUSIC_ARTIST,
	BaseItemKind.MUSIC_ALBUM,
	BaseItemKind.PLAYLIST,
)

/**
 * Velella: the groups of the separated info row: episode label, year or date, community rating, critic rating, runtime,
 * series status, media details and the official rating as an outlined badge at the end. Absent data adds no group.
 */
private fun buildSeparatedGroups(
	item: BaseItemDto,
	mediaSource: MediaSourceInfo?,
	includeRuntime: Boolean,
): List<@Composable () -> Unit> = buildList<@Composable () -> Unit> {
	if (item.type == BaseItemKind.EPISODE) add { InfoRowSeasonEpisode(item) }
	if (item.premiereDate != null || item.productionYear != null) add { InfoRowDate(item) }
	item.communityRating?.let { rating -> add { InfoRowCommunityRating(rating / COMMUNITY_RATING_SCALE) } }
	item.criticRating?.let { rating -> add { InfoRowCriticRating(rating / CRITIC_RATING_SCALE) } }
	if (includeRuntime) item.runTimeTicks?.ticks?.let { runtime -> add { BaseItemInfoRowRuntime(runtime) } }
	if (item.status?.let(SeriesStatus::fromNameOrNull) != null) add { InfoRowSeriesStatus(item) }
	if (mediaSource?.mediaStreams?.isNotEmpty() == true) {
		add { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { InfoRowMediaDetails(mediaSource) } }
	}
	item.officialRating?.takeIf { it.isNotBlank() }?.let { rating -> add { InfoRowOutlinedBadge(rating) } }
}

/**
 * Exposes the [BaseItemInfoRow] composable as Android view.
 */
class BaseItemInfoRowView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {
	private val _item = MutableStateFlow<BaseItemDto?>(null)
	private val _mediaSource = MutableStateFlow<MediaSourceInfo?>(null)
	private val _includeRuntime = MutableStateFlow(false)
	private val _separated = MutableStateFlow(false)

	var item: BaseItemDto?
		get() = _item.value
		set(value) {
			_item.value = value
		}

	var mediaSource: MediaSourceInfo?
		get() = _mediaSource.value
		set(value) {
			_mediaSource.value = value
		}

	var includeRuntime: Boolean
		get() = _includeRuntime.value
		set(value) {
			_includeRuntime.value = value
		}

	/** Velella: groups divided by vertical lines and an outlined official rating, used by the details screen. */
	var separated: Boolean
		get() = _separated.value
		set(value) {
			_separated.value = value
		}

	init {
		isFocusable = false
		descendantFocusability = FOCUS_BLOCK_DESCENDANTS
	}

	@Composable
	override fun Content() {
		val item by _item.collectAsState()
		val mediaSource by _mediaSource.collectAsState()
		val includeRuntime by _includeRuntime.collectAsState()
		val separated by _separated.collectAsState()

		// Velella: the theme's tokens for the info row items
		VelellaTheme { item?.let { BaseItemInfoRow(it, mediaSource, includeRuntime, separated) } }
	}
}
