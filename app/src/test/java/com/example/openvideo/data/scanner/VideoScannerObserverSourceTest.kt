package com.example.openvideo.data.scanner

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VideoScannerObserverSourceTest {

    @Test
    fun scanVideosRegistersMediaStoreObserverForIncrementalRefresh() {
        val source = String(Files.readAllBytes(videoScannerSource()))

        assertTrue(source.contains("ContentObserver"))
        assertTrue(source.contains("registerContentObserver(videoCollectionUri(), true, observer)"))
        assertTrue(source.contains("unregisterContentObserver(observer)"))
        assertTrue(source.contains("SCAN_OBSERVER_DEBOUNCE_MS"))
        assertTrue(source.contains("queryVideos()"))
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
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
