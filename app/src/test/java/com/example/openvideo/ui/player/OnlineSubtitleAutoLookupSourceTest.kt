package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class OnlineSubtitleAutoLookupSourceTest {

    @Test
    fun automaticSubtitleLoadPathDoesNotTriggerOnlineSubtitleSearch() {
        val viewModel = readSource("PlayerViewModel.kt")
        val controller = readSource("PlayerSubtitleController.kt")
        val loadBlock = viewModel.substringAfter("fun loadSubtitles(")
            .substringBefore("\n    fun getCurrentSubtitle()")
        val asyncBlock = controller.substringAfter("fun loadSubtitlesAsync(")
            .substringBefore("\n    fun registerPrefsListener()")

        assertFalse(loadBlock.contains("OnlineSubtitle"))
        assertFalse(asyncBlock.contains("OnlineSubtitle"))
        assertFalse(loadBlock.contains("OpenSubtitles"))
        assertFalse(asyncBlock.contains("OpenSubtitles"))
    }

    @Test
    fun onlineSubtitleClientBoundaryStaysInCoreSubtitlePackage() {
        val source = readSource("..", "core", "subtitle", "OnlineSubtitleClient.kt")

        assertTrue(source.contains("interface OnlineSubtitleClient"))
        assertTrue(source.contains("suspend fun search(request: OnlineSubtitleSearchRequest)"))
        assertTrue(source.contains("suspend fun download(request: OnlineSubtitleDownloadRequest)"))
    }

    private fun readSource(fileName: String): String =
        readSource("ui", "player", fileName)

    private fun readSource(vararg segments: String): String {
        val relativePath = if (segments.firstOrNull() == "..") {
            sourceRoot().resolve(segments.drop(1).joinToString(java.io.File.separator))
        } else {
            sourceRoot().resolve(segments.toList().joinToString(java.io.File.separator))
        }
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }

    private fun sourceRoot(): Path =
        Paths.get("src", "main", "java", "com", "example", "openvideo")
}
