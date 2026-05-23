package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerQuickEntrySourceTest {

    @Test
    fun landscapeSubtitleButtonUsesDedicatedQuickDialog() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter(
            "findViewById<View>(R.id.btn_land_subtitles)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {"
        ).substringBefore("\n        }\n\n        findViewById<View>(R.id.btn_land_pip_float)")

        assertTrue(block.contains("showSubtitleQuickDialog()"))
        assertFalse(block.contains("pickSubtitleLauncher.launch"))
    }

    @Test
    fun portraitQuickButtonsUseDedicatedAudioAndSubtitleDialogs() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val audioButtonBlock = source.substringAfter(
            "findViewById<View>(R.id.portrait_btn_quality)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {"
        ).substringBefore("\n        }\n\n        findViewById<View>(R.id.portrait_btn_episodes)")
        val subtitleButtonBlock = source.substringAfter(
            "findViewById<View>(R.id.portrait_btn_subtitles)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {"
        ).substringBefore("\n        }\n\n        btnScreenshot?.setPlayerClickListener")

        assertTrue(audioButtonBlock.contains("showAudioTrackQuickDialog()"))
        assertFalse(audioButtonBlock.contains("player_quality_local_single"))

        assertTrue(subtitleButtonBlock.contains("showSubtitleQuickDialog()"))
        assertFalse(subtitleButtonBlock.contains("pickSubtitleLauncher.launch"))
    }

    @Test
    fun playerActivityDelegatesQuickEntryStateToPolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val audioDialogBlock = source.substringAfter("private fun showAudioTrackQuickDialog() {")
            .substringBefore("\n    private fun showSubtitleQuickDialog()")
        val subtitleDialogBlock = source.substringAfter("private fun showSubtitleQuickDialog() {")
            .substringBefore("\n    private fun showQuickEntryDialog")

        assertTrue(audioDialogBlock.contains("PlayerQuickEntryPolicy.audioEntry("))
        assertTrue(subtitleDialogBlock.contains("PlayerQuickEntryPolicy.subtitleEntry("))
        assertTrue(subtitleDialogBlock.contains("subtitleDelayMs = playerPrefs.subtitleDelayMs"))
        assertTrue(subtitleDialogBlock.contains("PlayerQuickEntryAction.SubtitleDelayStatus"))
        assertTrue(subtitleDialogBlock.contains("player_quick_subtitle_delay_current"))
        assertTrue(subtitleDialogBlock.contains("PlayerQuickEntryAction.AdjustSubtitleDelay"))
        assertTrue(subtitleDialogBlock.contains("PlayerQuickEntryAction.ResetSubtitleDelay"))
        assertTrue(subtitleDialogBlock.contains("playerPrefs.subtitleDelayMs += action.deltaMs"))
        assertTrue(subtitleDialogBlock.contains("playerPrefs.subtitleDelayMs = 0"))
        assertTrue(subtitleDialogBlock.contains("applySubtitlePresentation()"))
        assertTrue(subtitleDialogBlock.contains("PlayerQuickEntryAction.OpenSubtitleSettings"))
        assertTrue(subtitleDialogBlock.contains("openSubtitleSettingsSheet()"))
        assertFalse(subtitleDialogBlock.contains("OpenSubtitleSettings ->\n                    openPlayerSettingsDialog()"))
    }

    @Test
    fun quickDialogsUseSingleActiveDialogGate() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val sessionListBlock = source.substringAfter("private fun showSessionVideoListPanel()")
            .substringBefore("\n    /** 与横屏齿轮按钮相同")
        val settingsBlock = source.substringAfter("private fun openPlayerSettingsDialog()")
            .substringBefore("\n    private fun showAspectRatioQuickDialog()")
        val aspectBlock = source.substringAfter("private fun showAspectRatioQuickDialog()")
            .substringBefore("\n    private fun showSpeedPickerDialog()")
        val speedBlock = source.substringAfter("private fun showSpeedPickerDialog()")
            .substringBefore("\n    /**\n     * 让快速选择型 AlertDialog")
        val quickEntryBlock = source.substringAfter("private fun showQuickEntryDialog(")
            .substringBefore("\n    private fun setupControls()")

        assertTrue(source.contains("private var activePlayerDialog: Dialog? = null"))
        assertTrue(source.contains("private fun showExclusivePlayerDialog("))
        assertTrue(sessionListBlock.contains("showExclusivePlayerDialog"))
        assertTrue(settingsBlock.contains("showExclusivePlayerDialog"))
        assertTrue(aspectBlock.contains("showExclusivePlayerDialog"))
        assertTrue(speedBlock.contains("showExclusivePlayerDialog"))
        assertTrue(quickEntryBlock.contains("showExclusivePlayerDialog"))
        assertTrue(aspectBlock.indexOf("showExclusivePlayerDialog") < aspectBlock.indexOf("PlayerGlassSheetDialog.showSingleChoice("))
        assertTrue(speedBlock.indexOf("showExclusivePlayerDialog") < speedBlock.indexOf("PlayerGlassSheetDialog.showSingleChoice("))
        assertTrue(quickEntryBlock.indexOf("showExclusivePlayerDialog") < quickEntryBlock.indexOf("PlayerGlassSheetDialog.showSingleChoice("))
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
