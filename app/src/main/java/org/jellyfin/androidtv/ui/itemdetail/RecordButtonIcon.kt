package org.jellyfin.androidtv.ui.itemdetail

import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.ImageViewCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.TextUnderButton

/**
 * Velella: shows a record button's icon in the danger colour while its recording is [set], and tinted like the other buttons
 * otherwise. The "set" drawables carry their own colour, so the button's tint is removed while they are shown.
 */
fun TextUnderButton.setRecordingSet(set: Boolean, @DrawableRes normal: Int, @DrawableRes setIcon: Int) {
	val image = binding.imageButton
	image.setImageResource(if (set) setIcon else normal)
	ImageViewCompat.setImageTintList(
		image,
		if (set) null else AppCompatResources.getColorStateList(context, R.drawable.button_icon_tint),
	)
}
