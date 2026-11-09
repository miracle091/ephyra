package org.jellyfin.androidtv.ui.composable.modifier

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

/**
 * Ephyra: pans and zooms the content very slowly (see [BackdropPanSpec]), only with a layer transform: no re-layout, no new
 * decode. The progress is read while drawing, so the frames do not recompose. It stops while the lifecycle is below RESUMED and
 * goes on from where it was; [key] restarts it (another title). [direction] comes from [BackdropPanSpec.direction]. [activeState] is
 * the lowest state that pans: a dream (the long pause screen) never gets past STARTED.
 */
@Composable
fun Modifier.backdropPan(direction: Int, key: Any? = null, activeState: Lifecycle.State = Lifecycle.State.RESUMED): Modifier {
	val context = LocalContext.current
	val enabled = remember {
		BackdropPanSpec.isEnabled(Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
	}
	if (!enabled) return this

	val progress = remember(key) { Animatable(0f) }
	val lifecycle = LocalLifecycleOwner.current.lifecycle
	LaunchedEffect(progress, lifecycle) {
		lifecycle.repeatOnLifecycle(activeState) {
			val remaining = BackdropPanSpec.remainingMs(progress.value)
			if (remaining > 0) progress.animateTo(1f, tween(remaining, easing = LinearEasing))
		}
	}

	return graphicsLayer {
		val value = progress.value
		val scale = BackdropPanSpec.scale(value)
		scaleX = scale
		scaleY = scale
		translationX = direction * BackdropPanSpec.SHIFT_X * size.width * value
		translationY = direction * BackdropPanSpec.SHIFT_Y * size.height * value
		transformOrigin = TransformOrigin.Center
	}
}
