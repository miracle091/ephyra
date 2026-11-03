package org.jellyfin.androidtv.di

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private const val LARGE_MEMORY_CLASS_MB = 512

class ImageMemoryCacheTest : FunSpec({
	test("The image memory cache takes 15% of the large heap") {
		val info = mockk<ApplicationInfo>()
		info.flags = ApplicationInfo.FLAG_LARGE_HEAP
		val activityManager = mockk<ActivityManager> {
			every { largeMemoryClass } returns LARGE_MEMORY_CLASS_MB
			every { memoryClass } returns LARGE_MEMORY_CLASS_MB / 2
		}
		val context = mockk<Context> {
			every { applicationInfo } returns info
			every { getSystemService(any<String>()) } returns activityManager
			every { getSystemService(ActivityManager::class.java) } returns activityManager
		}

		imageMemoryCache(context).maxSize shouldBe (LARGE_MEMORY_CLASS_MB * 1024L * 1024L * 0.15).toLong()
	}
})
