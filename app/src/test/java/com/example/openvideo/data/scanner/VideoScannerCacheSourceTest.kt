package com.example.openvideo.data.scanner

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VideoScannerCacheSourceTest {

    @Test
    fun deletePathsInvalidateScannerCache() {
        val source = String(Files.readAllBytes(videoScannerSource()))

        assertTrue(source.contains("removeCachedVideo"))
        assertTrue(source.contains("cacheMutex"))
        assertTrue(source.contains("SQLITE_MAX_VARIABLES"))
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
