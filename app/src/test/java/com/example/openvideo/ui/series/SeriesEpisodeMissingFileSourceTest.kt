package com.example.openvideo.ui.series

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesEpisodeMissingFileSourceTest {

    @Test
    fun watchStatusLabelsCanReportMissingFilesBeforeProgress() {
        val source = sourceText("SeriesEpisodeWatchStatusPolicy.kt")

        assertTrue(source.contains("val missingFile: String"))
        assertTrue(source.contains("R.string.history_continue_missing_file"))
        assertTrue(source.contains("isAvailable: Boolean = true"))
        assertTrue(source.contains("if (!isAvailable) return labels.missingFile"))
    }

    @Test
    fun availabilityPolicyReusesLocalMediaPlayableRules() {
        val source = sourceText("SeriesEpisodeAvailabilityPolicy.kt")

        assertTrue(source.contains("object SeriesEpisodeAvailabilityPolicy"))
        assertTrue(source.contains("LocalMediaUriPolicy.isPlayable(videoPath)"))
    }

    private fun sourceText(name: String): String =
        sourceFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun sourceFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "series", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
