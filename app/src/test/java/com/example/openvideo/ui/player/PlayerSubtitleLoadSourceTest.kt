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

        assertTrue(block.contains("PlayerSubtitleLoadCoordinator.loadWithOutcome("))
        assertTrue(block.contains("PlayerSubtitleLoadApplyPolicy.afterLoad("))
        assertTrue(block.contains("setSubtitles(outcome.subtitles)"))
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

    @Test
    fun sidecarCandidateChoicesAreSurfacedInsteadOfDropped() {
        val coordinator = String(Files.readAllBytes(kotlinSource("PlayerSubtitleLoadCoordinator.kt")))
        val viewModel = String(Files.readAllBytes(playerViewModelSource()))
        val controller = String(Files.readAllBytes(playerSubtitleControllerSource()))

        assertTrue(coordinator.contains("sealed interface PlayerSubtitleLoadOutcome"))
        assertTrue(coordinator.contains("data class RequiresUserChoice"))
        assertTrue(coordinator.contains("fun loadWithOutcome("))
        assertTrue(coordinator.contains("PlayerSubtitleLoadOutcome.RequiresUserChoice(selection.candidates)"))
        assertTrue(viewModel.contains("onCandidateChoiceRequired: (List<SubtitleCandidate>) -> Unit"))
        assertTrue(viewModel.contains("is PlayerSubtitleLoadOutcome.RequiresUserChoice"))
        assertTrue(viewModel.contains("onCandidateChoiceRequired(outcome.candidates)"))
        assertTrue(controller.contains("showSubtitleCandidateChoiceDialog"))
        assertTrue(controller.contains("MaterialAlertDialogBuilder"))
        assertTrue(controller.contains(".setItems("))
        assertTrue(controller.contains("playerPrefs.externalSubtitleUri = candidate.path"))
    }

    @Test
    fun sidecarCandidateChoiceDialogRequestsDefaultFocusForRemoteUse() {
        val controller = String(Files.readAllBytes(playerSubtitleControllerSource()))
        val block = controller.substringAfter("private fun showSubtitleCandidateChoiceDialog(candidates: List<SubtitleCandidate>) {")
            .substringBefore("\n    fun registerPrefsListener()")

        assertTrue(block.contains("val dialog = MaterialAlertDialogBuilder(activity)"))
        assertTrue(block.contains("dialog.listView?.post"))
        assertTrue(block.contains("dialog.listView?.requestFocus()"))
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
