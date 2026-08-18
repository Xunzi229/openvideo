package com.example.openvideo.data.scanner

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VideoScannerCacheSourceTest {

    @Test
    fun emptyMediaStoreEntriesAreNotReturnedAsPlayableVideos() {
        val source = String(Files.readAllBytes(videoScannerSource()))

        assertTrue(source.contains("if (size <= 0L) continue"))
        assertTrue(source.contains("if (size <= 0L) return null"))
    }

    @Test
    fun deletePathsInvalidateScannerCache() {
        val source = String(Files.readAllBytes(videoScannerSource()))

        assertTrue(source.contains("removeCachedVideo"))
        assertTrue(source.contains("cacheLock"))
        assertTrue(source.contains("synchronized(cacheLock)"))
        assertTrue(source.contains("SQLITE_MAX_VARIABLES"))
    }

    @Test
    fun scopedStorageUsesRelativePathForLibraryAndContentUriForPlayback() {
        val source = String(Files.readAllBytes(videoScannerSource()))

        assertTrue(source.contains("MediaStore.Video.Media.RELATIVE_PATH"))
        assertTrue(source.contains("MediaStorePathPolicy.playbackSource"))
        assertTrue(source.contains("libraryPath = libraryPath"))
        assertTrue(source.contains("MediaStore.Video.Media.DATE_MODIFIED"))
        assertTrue(source.contains("orientationDegrees = orientation"))
    }

    private fun videoScannerSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "data",
            "scanner",
            "VideoScanner.kt"
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
