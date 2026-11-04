package org.jellyfin.androidtv.benchmark

import android.net.Uri

/**
 * Ephyra (debug builds only): Jellyfin's standard test clips (https://repo.jellyfin.org/test-videos/, CC BY-SA 4.0), one moderate
 * 1080p and one heavy 4K file per format. 8K files are left out: no Shield outputs 8K.
 */
data class BenchmarkClip(val folder: String, val fileName: String) {
	val label: String get() = fileName.removePrefix("Test Jellyfin ").removeSuffix(".mp4")
	val url: String get() = "$BASE_URL/${Uri.encode(folder, "/")}/${Uri.encode(fileName)}"

	private companion object {
		const val BASE_URL = "https://repo.jellyfin.org/test-videos"
	}
}

val benchmarkClips = listOf(
	BenchmarkClip("SDR/AVC", "Test Jellyfin 1080p AVC 10M.mp4"),
	BenchmarkClip("SDR/AVC", "Test Jellyfin 4K AVC 100M.mp4"),
	BenchmarkClip("SDR/HEVC 8bit", "Test Jellyfin 1080p HEVC 8bit 10M.mp4"),
	BenchmarkClip("SDR/HEVC 8bit", "Test Jellyfin 4K HEVC 8bit 100M.mp4"),
	BenchmarkClip("SDR/HEVC 10bit", "Test Jellyfin 1080p HEVC 10bit 10M.mp4"),
	BenchmarkClip("SDR/HEVC 10bit", "Test Jellyfin 4K HEVC 10bit 100M.mp4"),
	BenchmarkClip("SDR/AV1", "Test Jellyfin 1080p AV1 10bit 10M.mp4"),
	BenchmarkClip("SDR/AV1", "Test Jellyfin 4K AV1 10bit 100M.mp4"),
	BenchmarkClip("HDR/HDR10/HEVC", "Test Jellyfin 1080p HEVC HDR10 10M.mp4"),
	BenchmarkClip("HDR/HDR10/HEVC", "Test Jellyfin 4K HEVC HDR10 100M.mp4"),
	BenchmarkClip("HDR/HDR10/AV1", "Test Jellyfin 1080p AV1 HDR10 10M.mp4"),
	BenchmarkClip("HDR/HDR10/AV1", "Test Jellyfin 4K AV1 HDR10 100M.mp4"),
	BenchmarkClip("HDR/Dolby Vision", "Test Jellyfin 1080p DV P5.mp4"),
	BenchmarkClip("HDR/Dolby Vision", "Test Jellyfin 4K DV P5.mp4"),
	BenchmarkClip("HDR/Dolby Vision", "Test Jellyfin 1080p DV P8.1.mp4"),
	BenchmarkClip("HDR/Dolby Vision", "Test Jellyfin 4K DV P8.1.mp4"),
	BenchmarkClip("HDR/Dolby Vision", "Test Jellyfin 1080p DV P8.4.mp4"),
	BenchmarkClip("HDR/Dolby Vision", "Test Jellyfin 4K DV P8.4.mp4"),
)
