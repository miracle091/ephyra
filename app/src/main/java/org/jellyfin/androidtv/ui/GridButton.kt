package org.jellyfin.androidtv.ui

import androidx.annotation.DrawableRes

open class GridButton @JvmOverloads constructor(
	val id: Int,
	val text: String,
	@DrawableRes val imageRes: Int? = null,
	/** Ephyra: a white vector shown in the middle of the button's colour tone, when it has no [imageRes] picture. */
	@DrawableRes val iconRes: Int? = null,
) {
	override fun toString() = text
}
