package org.jellyfin.design.token

import androidx.compose.ui.graphics.Color

/**
 * Ephyra: the colours of the Ephyra design system for one theme. Names follow its tokens (`surface-1` is [surface1]); `focus` is
 * [ink] and `on-focus` is [bg].
 */
data class EphyraPalette(
	val bg: Color,
	val surface1: Color,
	val surface2: Color,
	val surface3: Color,
	val ink: Color,
	val inkMuted: Color,
	val inkFaint: Color,
	val accent: Color,
	val onAccent: Color,
	val accentSoft: Color,
	val danger: Color,
	val onDanger: Color,
	val scrim: Color,
)

object EphyraColorTokens {
	/** Night blue, the default theme. */
	val dark = EphyraPalette(
		bg = Color(0xff0a0f24),
		surface1 = Color(0xff121a36),
		surface2 = Color(0xff1b2547),
		surface3 = Color(0xff26315a),
		ink = Color(0xfff2f4fa),
		inkMuted = Color(0xffb4bcd6),
		inkFaint = Color(0xff8c96b8),
		accent = Color(0xff2bb5e8),
		onAccent = Color(0xff04131f),
		accentSoft = Color(0xff0f3550),
		danger = Color(0xffff7a7a),
		onDanger = Color(0xff2a0606),
		scrim = Color(0xb8050814),
	)

	/** Dark with true black backgrounds and near-black surfaces, for OLED TVs (same greys as theme_ephyra_black.xml). */
	val black = dark.copy(
		bg = Color(0xff000000),
		surface1 = Color(0xff0a0a0a),
		surface2 = Color(0xff141414),
		surface3 = Color(0xff2e2e2e),
	)
}
