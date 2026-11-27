package org.jellyfin.androidtv.ui.player.video

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.asTimerInfoDto
import org.jellyfin.androidtv.ui.livetv.TvManager
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.compose.koinInject
import timber.log.Timber

/** Velella: whether the programme on air is set to record, for the live player's Record button. */
val BaseItemDto.isSetToRecord: Boolean get() = timerId != null

/**
 * Velella: the live player's Record button, as the legacy player's (CustomPlaybackOverlayFragment.toggleRecording): records the
 * programme on air (asking whether to record the whole series for a series), or cancels its recording (asking whether to cancel the
 * series' recordings too). [onChanged] runs after the server took the change, to look the programme up again.
 */
@Composable
fun rememberLiveRecordingToggle(
	onChanged: () -> Unit,
	api: ApiClient = koinInject(),
): (BaseItemDto) -> Unit {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val currentOnChanged by rememberUpdatedState(onChanged)
	return remember(context, scope, api) {
		{ program -> LiveRecording(context, scope, api) { currentOnChanged() }.toggle(program) }
	}
}

private class LiveRecording(
	private val context: Context,
	private val scope: CoroutineScope,
	private val api: ApiClient,
	private val onChanged: () -> Unit,
) {
	fun toggle(program: BaseItemDto) {
		val timerId = program.timerId
		val seriesTimerId = program.seriesTimerId
		when {
			timerId != null && seriesTimerId != null -> AlertDialog.Builder(context)
				.setTitle(R.string.lbl_cancel_recording)
				.setMessage(R.string.msg_cancel_entire_series)
				.setPositiveButton(R.string.lbl_cancel_series) { _, _ -> cancel { api.liveTvApi.cancelSeriesTimer(seriesTimerId) } }
				.setNegativeButton(R.string.just_one) { _, _ -> cancel { api.liveTvApi.cancelTimer(timerId) } }
				.show()

			timerId != null -> AlertDialog.Builder(context)
				.setTitle(R.string.lbl_cancel_recording)
				.setPositiveButton(R.string.lbl_yes) { _, _ -> cancel { api.liveTvApi.cancelTimer(timerId) } }
				.setNegativeButton(R.string.lbl_no, null)
				.show()

			program.isSeries == true -> AlertDialog.Builder(context)
				.setTitle(R.string.lbl_record_series)
				.setMessage(R.string.msg_record_entire_series)
				.setPositiveButton(R.string.lbl_record_series) { _, _ -> record(program, series = true) }
				.setNegativeButton(R.string.lbl_just_this_once) { _, _ -> record(program, series = false) }
				.show()

			else -> record(program, series = false)
		}
	}

	private fun record(program: BaseItemDto, series: Boolean) = run(R.string.msg_set_to_record, R.string.msg_unable_to_create_recording) {
		val defaultTimer = api.liveTvApi.getDefaultTimer(program.id.toString()).content
		if (series) api.liveTvApi.createSeriesTimer(defaultTimer)
		else api.liveTvApi.createTimer(defaultTimer.asTimerInfoDto())
	}

	private fun cancel(request: suspend () -> Unit) = run(R.string.msg_recording_cancelled, R.string.msg_unable_to_cancel, request)

	private fun run(@StringRes success: Int, @StringRes failure: Int, request: suspend () -> Unit) {
		scope.launch {
			val done = try {
				withContext(Dispatchers.IO) { request() }
				true
			} catch (error: CancellationException) {
				throw error
			} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
				Timber.w(error, "Unable to change the recording")
				false
			}

			Toast.makeText(context, if (done) success else failure, Toast.LENGTH_LONG).show()
			if (done) {
				TvManager.forceReload()
				onChanged()
			}
		}
	}
}
