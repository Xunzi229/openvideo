package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerQuickEntrySourceTest {

    @Test
    fun playerButtonsDelegateToQuickDialogControllerWrappers() {
        val source = read(playerActivitySource())

        assertTrue(source.contains("private val quickDialogs by lazy"))
        assertTrue(source.contains("private fun showAudioTrackQuickDialog() = quickDialogs.showAudioTrackQuickDialog()"))
        assertTrue(source.contains("private fun showSubtitleQuickDialog() = quickDialogs.showSubtitleQuickDialog()"))
        assertTrue(source.contains("private fun showSpeedPickerDialog() = quickDialogs.showSpeedPickerDialog()"))
        assertTrue(source.contains("private fun showAspectRatioQuickDialog() = quickDialogs.showAspectRatioQuickDialog()"))
        assertTrue(source.contains("private fun openPlayerSettingsDialog() = quickDialogs.openPlayerSettingsDialog()"))
        assertTrue(source.contains("private fun showSessionVideoListPanel() = quickDialogs.showSessionVideoListPanel()"))
    }

    @Test
    fun playerQuickDialogControllerDelegatesQuickEntryStateToPolicy() {
        val source = read(playerQuickDialogControllerSource())
        val audioDialogBlock = source.substringAfter("fun showAudioTrackQuickDialog() {")
            .substringBefore("\n    fun showSubtitleQuickDialog()")
        val subtitleDialogBlock = source.substringAfter("fun showSubtitleQuickDialog() {")
            .substringBefore("\n    fun openSubtitleSettingsSheet()")

        assertTrue(audioDialogBlock.contains("PlayerQuickEntryPolicy.audioEntry("))
        assertTrue(subtitleDialogBlock.contains("PlayerQuickEntryPolicy.subtitleEntry("))
        assertTrue(subtitleDialogBlock.contains("subtitleDelayMs = playerPrefs.subtitleDelayMs"))
        assertTrue(subtitleDialogBlock.contains("PlayerQuickEntryAction.SubtitleDelayStatus"))
        assertTrue(subtitleDialogBlock.contains("player_quick_subtitle_delay_current"))
        assertTrue(subtitleDialogBlock.contains("PlayerQuickEntryAction.AdjustSubtitleDelay"))
        assertTrue(subtitleDialogBlock.contains("PlayerQuickEntryAction.ResetSubtitleDelay"))
        assertTrue(subtitleDialogBlock.contains("playerPrefs.subtitleDelayMs += action.deltaMs"))
        assertTrue(subtitleDialogBlock.contains("playerPrefs.subtitleDelayMs = 0"))
        assertTrue(subtitleDialogBlock.contains("onApplySubtitlePresentation()"))
        assertTrue(subtitleDialogBlock.contains("PlayerQuickEntryAction.OpenSubtitleSettings"))
        assertTrue(subtitleDialogBlock.contains("openSubtitleSettingsSheet()"))
        assertFalse(subtitleDialogBlock.contains("openPlayerSettingsDialog()"))
    }

    @Test
    fun quickDialogsUseSingleActiveDialogGate() {
        val source = read(playerQuickDialogControllerSource())
        val sessionListBlock = source.substringAfter("fun showSessionVideoListPanel()")
            .substringBefore("\n    fun openPlayerSettingsDialog()")
        val settingsBlock = source.substringAfter("fun openPlayerSettingsDialog()")
            .substringBefore("\n    fun showAspectRatioQuickDialog()")
        val aspectBlock = source.substringAfter("fun showAspectRatioQuickDialog()")
            .substringBefore("\n    fun showSpeedPickerDialog()")
        val speedBlock = source.substringAfter("fun showSpeedPickerDialog()")
            .substringBefore("\n    fun showAudioTrackQuickDialog()")
        val quickEntryBlock = source.substringAfter("private fun showQuickEntryDialog(")
            .substringBefore("\n    private fun showExclusivePlayerDialog(")

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
        val source = read(playerQuickDialogControllerSource())
        val glassSheet = read(playerGlassSheetDialogSource())
        val speedBlock = source.substringAfter("fun showSpeedPickerDialog()")
            .substringBefore("\n    fun showAudioTrackQuickDialog()")
        val quickEntryBlock = source.substringAfter("private fun showQuickEntryDialog(")
            .substringBefore("\n    private fun showExclusivePlayerDialog(")

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
        val source = read(playerQuickDialogControllerSource())
        val glassSheet = read(playerGlassSheetDialogSource())
        val aspectBlock = source.substringAfter("fun showAspectRatioQuickDialog()")
            .substringBefore("\n    fun showSpeedPickerDialog()")
        val speedBlock = source.substringAfter("fun showSpeedPickerDialog()")
            .substringBefore("\n    fun showAudioTrackQuickDialog()")
        val quickEntryBlock = source.substringAfter("private fun showQuickEntryDialog(")
            .substringBefore("\n    private fun showExclusivePlayerDialog(")

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
        val source = read(playerQuickDialogControllerSource())
        val speedBlock = source.substringAfter("fun showSpeedPickerDialog()")
            .substringBefore("\n    fun showAudioTrackQuickDialog()")
        val quickEntryBlock = source.substringAfter("private fun showQuickEntryDialog(")
            .substringBefore("\n    private fun showExclusivePlayerDialog(")

        assertTrue(speedBlock.indexOf("onHideControls()") in 0 until speedBlock.indexOf("PlayerGlassSheetDialog.showSingleChoice("))
        assertTrue(quickEntryBlock.indexOf("onHideControls()") in 0 until quickEntryBlock.indexOf("PlayerGlassSheetDialog.showSingleChoice("))
    }

    @Test
    fun aspectQuickDialogUsesSettingsOptionsAndSelectionPolicy() {
        val source = read(playerQuickDialogControllerSource())
        val aspectBlock = source.substringAfter("fun showAspectRatioQuickDialog()")
            .substringBefore("\n    fun showSpeedPickerDialog()")

        assertTrue(aspectBlock.contains("PlayerAspectRatioOptions.entries"))
        assertTrue(aspectBlock.contains("PlayerContentFrameSettingsPolicy.onAspectRatioSelected("))
        assertTrue(aspectBlock.contains("currentContentFrameMode = playerPrefs.contentFrameMode"))
        assertTrue(aspectBlock.contains("selection.contentFrameOverride?.let { playerPrefs.contentFrameMode = it }"))
        assertTrue(aspectBlock.contains("onAspectRatioChanged()"))
        assertFalse(aspectBlock.contains("AspectRatio.FIT to R.string.player_sheet_fit_screen"))
    }

    private fun read(path: Path): String = String(Files.readAllBytes(path))

    private fun playerActivitySource(): Path = sourceFile("PlayerActivity.kt")

    private fun playerQuickDialogControllerSource(): Path = sourceFile("PlayerQuickDialogController.kt")

    private fun playerGlassSheetDialogSource(): Path = sourceFile("PlayerGlassSheetDialog.kt")

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
