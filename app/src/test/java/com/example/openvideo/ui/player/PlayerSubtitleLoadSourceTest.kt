package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSubtitleLoadSourceTest {

    @Test
    fun playerActivityDelegatesSubtitleLoadToViewModelAndToastPolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter("private fun loadSubtitlesAsync(uriString: String, videoPath: String, showToast: Boolean = false) {")
            .substringBefore("\n    private fun initViews()")

        assertTrue(block.contains("viewModel.loadSubtitles("))
        assertTrue(block.contains("PlayerSubtitleLoadToastPolicy.messageRes("))
        assertFalse(block.contains("PlayerSubtitleLoadCoordinator.load("))
        assertFalse(block.contains("PlayerSubtitleLoadApplyPolicy.afterLoad("))
    }

    @Test
    fun playerViewModelOrchestratesSubtitleLoad() {
        val source = String(Files.readAllBytes(playerViewModelSource()))
        val block = source.substringAfter("fun loadSubtitles(")
            .substringBefore("\n    fun getCurrentSubtitle()")

        assertTrue(block.contains("PlayerSubtitleLoadCoordinator.load("))
        assertTrue(block.contains("PlayerSubtitleLoadApplyPolicy.afterLoad("))
        assertTrue(block.contains("setSubtitles(subtitles)"))
    }

    private fun playerViewModelSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerViewModel.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerActivitySource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerActivity.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
