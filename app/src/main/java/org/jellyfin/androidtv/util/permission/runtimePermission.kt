package org.jellyfin.androidtv.util.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * What a feature should do when it needs a runtime permission.
 */
enum class PermissionRequestAction {
	/** The permission is granted: go ahead. */
	PROCEED,

	/** Ask the user with the system dialog. */
	REQUEST,

	/** The user refused for good: report it instead of asking again. */
	DENIED,
}

/**
 * Decide what to do with a runtime permission. Ask on the first attempt and whenever the system says a rationale should be
 * shown; after that, a refusal is final.
 */
fun decidePermissionRequest(
	granted: Boolean,
	shouldShowRationale: Boolean,
	alreadyRequested: Boolean,
): PermissionRequestAction = when {
	granted -> PermissionRequestAction.PROCEED
	shouldShowRationale || !alreadyRequested -> PermissionRequestAction.REQUEST
	else -> PermissionRequestAction.DENIED
}

/**
 * State of one runtime permission, backed by the AndroidX Activity Result APIs.
 */
@Stable
class RuntimePermissionState(
	private val context: Context,
	private val activity: Activity?,
	val permission: String,
	private val launcher: ManagedActivityResultLauncher<String, Boolean>,
) {
	val isGranted: Boolean
		get() = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

	val shouldShowRationale: Boolean
		get() = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

	fun launchRequest() = launcher.launch(permission)
}

/**
 * Remember the state of [permission]. [onResult] receives whether the user granted it after [RuntimePermissionState.launchRequest].
 */
@Composable
fun rememberRuntimePermission(
	permission: String,
	onResult: (granted: Boolean) -> Unit,
): RuntimePermissionState {
	val context = LocalContext.current
	val activity = LocalActivity.current
	val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)

	return remember(context, activity, permission, launcher) {
		RuntimePermissionState(context, activity, permission, launcher)
	}
}
