package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.VelellaPreferences
import org.jellyfin.androidtv.ui.base.VelellaTheme
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalVelellaPalette
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.koin.compose.koinInject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SkipOverlayComposable(
	visible: Boolean,
) {
	Box(
		contentAlignment = Alignment.BottomEnd,
		modifier = Modifier
			// Velella: the controls' safe area (velella_side_margin, velella_safe_y)
			.padding(end = 32.dp, bottom = 27.dp)
	) {
		AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
			val palette = LocalVelellaPalette.current
			// Velella: the player's text pills (velella_osd_pill.xml): solid surface-2, a full pill, 48dp high, ink content
			Row(
				modifier = Modifier
					.alpha(skipButtonAlpha(visible)) // Velella: dims after a few seconds
					.height(48.dp)
					.clip(CircleShape)
					.background(palette.surface2)
					.padding(start = 16.dp, end = 20.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Icon(
					imageVector = ImageVector.vectorResource(R.drawable.ic_control_select),
					contentDescription = null,
					modifier = Modifier.size(24.dp),
					// Velella: a multi-colour drawable (disc, arrows, centre dot from the theme); a tint would flatten it into a solid disc
					tint = Color.Unspecified,
				)

				Text(
					text = stringResource(R.string.segment_action_skip),
					style = LocalTextStyle.current.merge(
						color = palette.ink,
						fontSize = 14.sp,
						fontWeight = FontWeight.Bold,
					),
					maxLines = 1,
				)
			}
		}
	}
}

class SkipOverlayView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {
	private val _currentPosition = MutableStateFlow(Duration.ZERO)
	private val _targetPosition = MutableStateFlow<Duration?>(null)
	private val _skipUiEnabled = MutableStateFlow(true)
	// Velella: hidden by itself after a while, but the segment is kept so the button can be called back
	private val _autoHidden = MutableStateFlow(false)

	var currentPosition: Duration
		get() = _currentPosition.value
		set(value) {
			_currentPosition.value = value
		}

	var currentPositionMs: Long
		get() = _currentPosition.value.inWholeMilliseconds
		set(value) {
			_currentPosition.value = value.milliseconds
		}

	var targetPosition: Duration?
		get() = _targetPosition.value
		set(value) {
			_autoHidden.value = false
			_targetPosition.value = value
		}

	var targetPositionMs: Long?
		get() = _targetPosition.value?.inWholeMilliseconds
		set(value) {
			_autoHidden.value = false
			_targetPosition.value = value?.milliseconds
		}

	var skipUiEnabled: Boolean
		get() = _skipUiEnabled.value
		set(value) {
			_skipUiEnabled.value = value
		}

	private val state: SkipButtonState
		get() = skipButtonState(_skipUiEnabled.value, _autoHidden.value, _targetPosition.value, _currentPosition.value)

	val visible: Boolean
		get() = state == SkipButtonState.SHOWN

	/** Velella: whether a key would call the button back (it hid by itself and the segment still lasts). */
	val recallable: Boolean
		get() = state == SkipButtonState.RECALLABLE

	/** Velella: shows the button again for another auto-hide period. */
	fun recall() {
		_autoHidden.value = false
	}

	@Composable
	override fun Content() {
		val skipUiEnabled by _skipUiEnabled.collectAsState()
		val currentPosition by _currentPosition.collectAsState()
		val targetPosition by _targetPosition.collectAsState()
		val autoHidden by _autoHidden.collectAsState()

		val visible by remember(skipUiEnabled, currentPosition, targetPosition, autoHidden) {
			derivedStateOf { visible }
		}

		// Velella: the user's choice of how long the button stays (no delay: until the segment ends)
		val preferences = koinInject<VelellaPreferences>()
		val autoHideDelay = skipAutoHideDelay(preferences[VelellaPreferences.skipAutoHideSeconds])

		// Auto hide; Velella: only hides, the segment stays so a key calls the button back
		LaunchedEffect(skipUiEnabled, targetPosition, autoHidden, autoHideDelay) {
			if (autoHidden || targetPosition == null || autoHideDelay == null) return@LaunchedEffect
			delay(autoHideDelay)
			_autoHidden.value = true
		}

		// Velella: the user's theme colours
		VelellaTheme {
			SkipOverlayComposable(visible)
		}
	}
}
