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
        val activitySource = String(Files.readAllBytes(playerActivitySource()))
        val source = String(Files.readAllBytes(playerSubtitleControllerSource()))
        val block = source.substringAfter("fun loadSubtitlesAsync(uriString: String, videoPath: String, showToast: Boolean = false) {")
            .substringBefore("\n    fun registerPrefsListener()")

        assertTrue(activitySource.contains("subtitles.loadSubtitlesAsync(uriString, videoPath, showToast)"))
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
        assertTrue(block.contains("rememberedSubtitlePath = playerPrefs.externalSubtitleUri"))
        assertTrue(block.contains("languagePreference = playerPrefs.subtitleLanguagePreference()"))
    }

    @Test
    fun sidecarLoadUsesCandidateSelectionPolicyBeforeLoadingFile() {
        val source = String(Files.readAllBytes(kotlinSource("PlayerSubtitleLoadCoordinator.kt")))
        val block = source.substringAfter("is PlayerSubtitleLoadRequest.SidecarFile -> {")
            .substringBefore("\n            is PlayerSubtitleLoadRequest.SubtitleUri")

        assertTrue(block.contains("loader.findSubtitleCandidates(request.videoPath)"))
        assertTrue(block.contains("SubtitleCandidateSelectionPolicy.select("))
        assertTrue(block.contains("is SubtitleCandidateSelection.AutoApply"))
        assertFalse(block.contains("subtitleFiles[0]"))
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
        return kotlinSource("PlayerActivity.kt")
    }

    private fun playerSubtitleControllerSource(): Path {
        return kotlinSource("PlayerSubtitleController.kt")
    }

    private fun kotlinSource(name: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            name
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
