package org.jellyfin.androidtv.di

import android.content.Context
import coil3.memory.MemoryCache

/**
 * Ephyra: Coil sizes its memory cache at 20% of the app's large memory class by default. Bitmaps live in native
 * memory, so this bounds total RAM use on the 3 GB Shield rather than the Java heap. Cards are requested at display
 * size, so 15% still holds several rows of artwork.
 */
const val IMAGE_MEMORY_CACHE_PERCENT = 0.15

fun imageMemoryCache(context: Context): MemoryCache = MemoryCache.Builder()
	.maxSizePercent(context, IMAGE_MEMORY_CACHE_PERCENT)
	.build()
