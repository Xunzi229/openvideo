package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerQuickEntrySourceTest {

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
