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

    @Test
    fun speedAudioAndSubtitleQuickDialogsUseResponsivePlayerChrome() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val glassSheet = String(Files.readAllBytes(playerGlassSheetDialogSource()))
        val speedBlock = source.substringAfter("private fun showSpeedPickerDialog()")
            .substringBefore("\n    private fun showExclusivePlayerDialog")
        val quickEntryBlock = source.substringAfter("private fun showQuickEntryDialog(")
            .substringBefore("\n    private fun setupControls()")

        assertTrue(source.contains("private fun quickChoiceChrome()"))
        assertTrue(source.contains("PlayerGlassSheetChrome.PLAYER_BOTTOM"))
        assertTrue(speedBlock.contains("chrome = quickChoiceChrome()"))
        assertTrue(quickEntryBlock.contains("chrome = quickChoiceChrome()"))
        assertTrue(glassSheet.contains("enum class PlayerGlassSheetChrome"))
        assertTrue(glassSheet.contains("PLAYER_BOTTOM"))
        assertTrue(glassSheet.contains("PLAYER_SETTINGS_PANEL"))
        assertTrue(glassSheet.contains("R.layout.dialog_player_quick_bottom_sheet"))
        assertTrue(glassSheet.contains("R.layout.item_player_quick_bottom_sheet_row"))
        assertTrue(glassSheet.contains("PlayerGlassSheetChrome.PLAYER_BOTTOM,"))
        assertTrue(glassSheet.contains("PlayerGlassSheetChrome.PLAYER_SETTINGS_PANEL -> Dialog(context).apply"))
        assertTrue(glassSheet.contains("requestWindowFeature(Window.FEATURE_NO_TITLE)"))
        assertTrue(glassSheet.contains("setContentView(content)"))
        assertTrue(glassSheet.contains("setGravity(Gravity.BOTTOM)"))
        assertTrue(glassSheet.contains("LayoutParams.MATCH_PARENT"))
        assertTrue(glassSheet.contains("applyBottomRowVisual("))
        assertFalse(glassSheet.substringAfter("private fun rowLayout(")
            .substringBefore("\n    private fun applyChoiceState")
            .contains("player_aspect_ratio_row_selected"))
    }

    @Test
    fun landscapeSubtitleAspectAndSpeedUseSettingsPanelChrome() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val glassSheet = String(Files.readAllBytes(playerGlassSheetDialogSource()))
        val aspectBlock = source.substringAfter("private fun showAspectRatioQuickDialog()")
            .substringBefore("\n    private fun handleSmartCropQuickToggle()")
        val speedBlock = source.substringAfter("private fun showSpeedPickerDialog()")
            .substringBefore("\n    private fun showExclusivePlayerDialog")
        val quickEntryBlock = source.substringAfter("private fun showQuickEntryDialog(")
            .substringBefore("\n    private fun setupControls()")

        assertTrue(source.contains("private fun quickChoiceChrome()"))
        assertTrue(source.contains("PlayerGlassSheetChrome.PLAYER_SETTINGS_PANEL"))
        assertTrue(aspectBlock.contains("chrome = quickChoiceChrome()"))
        assertTrue(aspectBlock.contains("playerPrefs = playerPrefs"))
        assertTrue(speedBlock.contains("chrome = quickChoiceChrome()"))
        assertTrue(speedBlock.contains("playerPrefs = playerPrefs"))
        assertTrue(quickEntryBlock.contains("chrome = quickChoiceChrome()"))
        assertTrue(quickEntryBlock.contains("playerPrefs = playerPrefs"))
        assertTrue(glassSheet.contains("PLAYER_SETTINGS_PANEL"))
        assertTrue(glassSheet.contains("PlayerSettingsSheetChrome.applyWindowLayout"))
        assertTrue(glassSheet.contains("PlayerSettingsSheetChrome.applyBackdrop"))
        assertTrue(glassSheet.contains("PlayerSettingsSheetChrome.applyPanelOpacity"))
    }

    @Test
    fun speedAudioAndSubtitleQuickDialogsHidePlayerControlsWhileOpen() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val speedBlock = source.substringAfter("private fun showSpeedPickerDialog()")
            .substringBefore("\n    private fun showExclusivePlayerDialog")
        val quickEntryBlock = source.substringAfter("private fun showQuickEntryDialog(")
            .substringBefore("\n    private fun setupControls()")

        assertTrue(speedBlock.indexOf("hideControls()") in 0 until speedBlock.indexOf("PlayerGlassSheetDialog.showSingleChoice("))
        assertTrue(quickEntryBlock.indexOf("hideControls()") in 0 until quickEntryBlock.indexOf("PlayerGlassSheetDialog.showSingleChoice("))
    }

    @Test
    fun aspectQuickDialogUsesSettingsOptionsAndSelectionPolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val aspectBlock = source.substringAfter("private fun showAspectRatioQuickDialog()")
            .substringBefore("\n    private fun showSpeedPickerDialog()")

        assertTrue(aspectBlock.contains("PlayerAspectRatioOptions.entries"))
        assertTrue(aspectBlock.contains("PlayerContentFrameSettingsPolicy.onAspectRatioSelected("))
        assertTrue(aspectBlock.contains("currentContentFrameMode = playerPrefs.contentFrameMode"))
        assertTrue(aspectBlock.contains("selection.contentFrameOverride?.let { playerPrefs.contentFrameMode = it }"))
        assertFalse(aspectBlock.contains("AspectRatio.FIT to R.string.player_sheet_fit_screen"))
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

    private fun playerGlassSheetDialogSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerGlassSheetDialog.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
