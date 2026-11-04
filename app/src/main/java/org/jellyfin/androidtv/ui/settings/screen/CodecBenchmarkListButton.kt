package org.jellyfin.androidtv.ui.settings.screen

import android.content.ComponentName
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.navigation.focus.focusKey

/** The benchmark activity exists in debug builds only (app/src/debug), so it is started by name. */
private const val CODEC_BENCHMARK_ACTIVITY = "org.jellyfin.androidtv.benchmark.CodecBenchmarkActivity"

/** Ephyra: opens the codec benchmark (guide 7.1); shown in debug builds only. */
@Composable
fun CodecBenchmarkListButton() {
	val context = LocalContext.current
	ListButton(
		headingContent = { Text(stringResource(R.string.ephyra_benchmark_title)) },
		captionContent = { Text(stringResource(R.string.ephyra_benchmark_description)) },
		onClick = { context.startActivity(Intent().setComponent(ComponentName(context, CODEC_BENCHMARK_ACTIVITY))) },
		modifier = Modifier.focusKey("ephyra_codec_benchmark"),
	)
}
