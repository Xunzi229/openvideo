package com.example.openvideo.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class OnlineSubtitleServiceSelectionSourceTest {

    @Test
    fun roadmapRecordsOnlineSubtitleServiceDecisionAndPrivacyBoundary() {
        val doc = loadText(Paths.get("docs", "roadmap", "online-subtitle-service-selection.md"))

        assertTrue(doc.contains("P4-ONLINE-001"))
        assertTrue(doc.contains("OpenSubtitles.com"))
        assertTrue(doc.contains("API key"))
        assertTrue(doc.contains("User-Agent"))
        assertTrue(doc.contains("manual search only"))
        assertTrue(doc.contains("No automatic lookup when opening a video"))
        assertTrue(doc.contains("Do not upload file hash by default"))
        assertTrue(doc.contains("P4-ONLINE-002"))
        assertTrue(doc.contains("Selected direction"))
    }

    @Test
    fun runtimeDoesNotAddOpenSubtitlesDependencyInSelectionSlice() {
        val build = loadText(Paths.get("app", "build.gradle.kts"))

        assertFalse(build.contains("opensubtitles", ignoreCase = true))
        assertFalse(build.contains("subtitleapi", ignoreCase = true))
    }

    private fun loadText(path: java.nio.file.Path): String {
        val resolved = sequenceOf(
            path,
            Paths.get("..").resolve(path),
            Paths.get("app").resolve(path)
        ).first(Files::exists)
        return String(Files.readAllBytes(resolved))
    }
}
