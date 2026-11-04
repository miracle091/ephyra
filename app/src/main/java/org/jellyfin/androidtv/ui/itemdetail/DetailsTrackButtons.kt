package org.jellyfin.androidtv.ui.itemdetail

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.TextUnderButton
import org.jellyfin.androidtv.ui.playback.SUBTITLES_OFF
import org.jellyfin.androidtv.ui.playback.TrackPreselection
import org.jellyfin.androidtv.ui.playback.overlay.action.preferredLanguagesFirst
import org.jellyfin.androidtv.ui.playback.playedMediaSource
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.koin.java.KoinJavaComponent

/**
 * Ephyra: "Audio" and "Subtitles" buttons on the details screen of a video, so the tracks are picked before playback and the
 * server builds the right stream at once (see [TrackPreselection]). Only for the built-in legacy player, which reads the pick.
 */
object DetailsTrackButtons {
	private val videoTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.VIDEO, BaseItemKind.MUSIC_VIDEO)

	/** Which buttons [item] gets: audio with more than one audio track, subtitles with at least one subtitle track. */
	fun trackTypesFor(item: BaseItemDto, builtInLegacyPlayer: Boolean): List<MediaStreamType> {
		if (!builtInLegacyPlayer || item.type !in videoTypes) return emptyList()
		val streams = item.playedMediaSource?.mediaStreams.orEmpty()
		return buildList {
			if (streams.count { it.type == MediaStreamType.AUDIO } > 1) add(MediaStreamType.AUDIO)
			if (streams.any { it.type == MediaStreamType.SUBTITLE }) add(MediaStreamType.SUBTITLE)
		}
	}

	/** The buttons for the item [currentItem] returns (read on each click, as the version menu can swap the item). */
	@JvmStatic
	fun create(context: Context, currentItem: () -> BaseItemDto, buttonSize: Int): List<TextUnderButton> {
		val preferences = KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)
		val builtInLegacyPlayer = !preferences[UserPreferences.useExternalPlayer] &&
			!preferences[UserPreferences.playbackRewriteVideoEnabled]

		return trackTypesFor(currentItem(), builtInLegacyPlayer).map { type ->
			val (icon, label) = if (type == MediaStreamType.AUDIO) R.drawable.ic_select_audio to R.string.pref_audio
			else R.drawable.ic_select_subtitle to R.string.pref_subtitles
			TextUnderButton.create(context, icon, buttonSize, 0, context.getString(label)) { view ->
				showTrackMenu(view, currentItem(), type)
			}
		}
	}

	private fun showTrackMenu(anchor: View, item: BaseItemDto, type: MediaStreamType) {
		val source = item.playedMediaSource ?: return
		val preselection = KoinJavaComponent.get<TrackPreselection>(TrackPreselection::class.java)
		val choice = preselection.choiceFor(item.id, source.id)
		val audio = type == MediaStreamType.AUDIO

		val streams = source.mediaStreams.orEmpty().filter { it.type == type }
			.preferredLanguagesFirst { if (audio) it.audioLanguagePreference else it.subtitleLanguagePreference }
		val current = if (audio) choice?.audioIndex ?: source.defaultAudioStreamIndex
		else choice?.subtitleIndex ?: source.defaultSubtitleStreamIndex ?: SUBTITLES_OFF

		PopupMenu(anchor.context, anchor, Gravity.END).apply {
			if (!audio) menu.add(0, SUBTITLES_OFF, 0, R.string.lbl_none)
			streams.forEachIndexed { order, stream -> menu.add(0, stream.index, order + 1, stream.label) }
			menu.setGroupCheckable(0, true, true)
			current?.let { menu.findItem(it) }?.isChecked = true

			setOnMenuItemClickListener { menuItem ->
				if (audio) preselection.pickAudio(item.id, source.id, menuItem.itemId)
				else preselection.pickSubtitle(item.id, source.id, menuItem.itemId)
				true
			}
		}.show()
	}

	private val MediaStream.label: String
		get() = displayTitle ?: title ?: language ?: "#$index"
}
