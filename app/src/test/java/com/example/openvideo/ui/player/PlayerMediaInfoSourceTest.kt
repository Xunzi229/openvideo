package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerMediaInfoSourceTest {

    @Test
    fun infoPageIncludesContainerAndTrackDetails() {
        val dialogSource = String(Files.readAllBytes(sourceFile("PlayerSettingsDialog.kt")))
        val mediaInfoSource = String(Files.readAllBytes(sourceFile("PlayerMediaInfo.kt")))

        assertTrue(dialogSource.contains("PlayerMediaInfoReader.read(context, viewModel.currentVideoSource())"))
        assertTrue(dialogSource.contains("mediaInfoRows("))
        assertTrue(mediaInfoSource.contains("player_settings_info_audio_compatibility"))

        listOf(
            "MediaExtractor()",
            "MediaMetadataRetriever()",
            "trackCount",
            "getTrackFormat",
            "MediaFormat.KEY_MIME",
            "MediaFormat.KEY_SAMPLE_RATE",
            "MediaFormat.KEY_CHANNEL_COUNT",
            "MediaFormat.KEY_BIT_RATE",
            "MediaFormat.KEY_FRAME_RATE"
        ).forEach { expected ->
            assertTrue("Missing media info extraction: $expected", mediaInfoSource.contains(expected))
        }
    }

    @Test
    fun mediaInfoRecognizesDtsAudioAsCompatibilityRisk() {
        val mediaInfoSource = String(Files.readAllBytes(sourceFile("PlayerMediaInfo.kt")))

        assertTrue(mediaInfoSource.contains("audio/vnd.dts"))
        assertTrue(mediaInfoSource.contains("audio/vnd.dts.hd"))
        assertTrue(mediaInfoSource.contains("audio/true-hd"))
        assertTrue(mediaInfoSource.contains("DTS"))
        assertTrue(mediaInfoSource.contains("DCA"))
        assertTrue(mediaInfoSource.contains("hasDtsAudio"))
    }

    @Test
    fun infoPageFallsBackToTrackResolutionWhenPlayerVideoSizeIsNotReady() {
        val dialogSource = String(Files.readAllBytes(sourceFile("PlayerSettingsDialog.kt")))
        val infoRows = dialogSource
            .substringAfter("private fun videoInfoRows()")
            .substringBefore("\n    private fun audioDiagnosticRows")
        val resolutionLabel = dialogSource
            .substringAfter("private fun videoResolutionLabel(")
            .substringBefore("\n    private fun copyVideoInfoToClipboard()")

        assertTrue(infoRows.contains("val mediaInfo = PlayerMediaInfoReader.read"))
        assertTrue(infoRows.contains("videoResolutionLabel(mediaInfo)"))
        assertTrue(resolutionLabel.contains("mediaInfo?.tracks"))
        assertTrue(resolutionLabel.contains("PlayerMediaTrack.Type.VIDEO"))
        assertTrue(resolutionLabel.contains("track.width"))
        assertTrue(resolutionLabel.contains("track.height"))
    }

    private fun sourceFile(fileName: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            fileName
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
