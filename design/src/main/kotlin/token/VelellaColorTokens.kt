// Design tokens: every literal is a named token value.
@file:Suppress("MagicNumber")

package org.jellyfin.design.token

import androidx.compose.ui.graphics.Color

/**
 * Velella: the colours of the Velella design system for one theme. Names follow its tokens (`surface-1` is [surface1]); `focus` is
 * [ink] and `on-focus` is [bg].
 */
data class VelellaPalette(
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

object VelellaColorTokens {
	/** `art-1` to `art-6`: the tones of covers without a picture, the same in every theme, always with `ink` text. */
	val art = listOf(
		Color(0xff155e63),
		Color(0xff8a3b2b),
		Color(0xff5b2c6f),
		Color(0xff2d3a8c),
		Color(0xff23573f),
		Color(0xff6e2440),
	)

	/** Night blue, the default theme. */
	val dark = VelellaPalette(
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

	/** Dark with true black backgrounds and near-black surfaces, for OLED TVs (same greys as theme_velella_black.xml). */
	val black = dark.copy(
		bg = Color(0xff000000),
		surface1 = Color(0xff0a0a0a),
		surface2 = Color(0xff141414),
		surface3 = Color(0xff2e2e2e),
	)

	/** Deep teal with a mint accent (same colours as theme_velella_abyss.xml). */
	val abyss = dark.copy(
		bg = Color(0xff06171a),
		surface1 = Color(0xff0d2428),
		surface2 = Color(0xff15333a),
		surface3 = Color(0xff1f444c),
		inkMuted = Color(0xffb0cccc),
		inkFaint = Color(0xff86a6a8),
		accent = Color(0xff3ddcb4),
		onAccent = Color(0xff032019),
		accentSoft = Color(0xff0e3d38),
		scrim = Color(0xb8020a0c),
	)

	/** Warm dark red with a coral accent (same colours as theme_velella_coral.xml). */
	val coral = dark.copy(
		bg = Color(0xff1a0e10),
		surface1 = Color(0xff261518),
		surface2 = Color(0xff341e22),
		surface3 = Color(0xff45292e),
		inkMuted = Color(0xffd6bcba),
		inkFaint = Color(0xffb0928f),
		accent = Color(0xffff8a65),
		onAccent = Color(0xff2a0e06),
		accentSoft = Color(0xff4a2219),
		scrim = Color(0xb80c0607),
	)

	/** Deep violet with a lilac accent (same colours as theme_velella_medusa.xml). */
	val medusa = dark.copy(
		bg = Color(0xff120c24),
		surface1 = Color(0xff1c1436),
		surface2 = Color(0xff271d47),
		surface3 = Color(0xff33285a),
		inkMuted = Color(0xffc4b8de),
		inkFaint = Color(0xff9c8fc0),
		accent = Color(0xffb794ff),
		onAccent = Color(0xff1a0b33),
		accentSoft = Color(0xff33215c),
		scrim = Color(0xb8080512),
	)
}
