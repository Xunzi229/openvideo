package com.example.openvideo.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SettingsLanguageRowSourceTest {

    @Test
    fun preferenceBackedValueRowsRefreshVisibleLabelsImmediatelyAfterSavingPreference() {
        val source = settingsFragmentSource()

        assertLabelRefreshesAfterSave(source, "viewModel.setThemeMode(modes[next])", "updateThemeLabel(tvTheme)")
        assertLabelRefreshesAfterSave(source, "viewModel.setLanguage(langs[next])", "updateLanguageLabel(tvLanguage)")
        assertLabelRefreshesAfterSave(source, "viewModel.setDefaultRatio(ratio)", "updateRatioLabel(tvRatio)")
        assertLabelRefreshesAfterSave(source, "viewModel.setDefaultSpeed(speed)", "updateSpeedLabel(tvSpeed)")
    }

    @Test
    fun defaultPlaybackRowsOpenGlassChoiceDialogsInsteadOfCyclingInline() {
        val source = settingsFragmentSource()
        val glassDialogSource = playerGlassSheetDialogSource()

        assertTrue(source.contains("showDefaultRatioDialog(tvRatio)"))
        assertTrue(source.contains("showDefaultSpeedDialog(tvSpeed)"))
        assertTrue(source.contains("PlayerGlassSheetDialog.showSingleChoice"))
        assertTrue(glassDialogSource.contains("R.layout.dialog_player_glass_sheet"))
        assertTrue(glassDialogSource.contains("R.layout.item_player_glass_sheet_row"))
        assertTrue(source.contains("DefaultPlayerSettings.supportedSpeeds"))
        assertTrue(source.contains("AspectRatio.entries"))
        assertTrue(source.contains("viewModel.setDefaultRatio(ratio)"))
        assertTrue(source.contains("viewModel.setDefaultSpeed(speed)"))
        assertFalse(source.contains("ratios[next]"))
        assertFalse(source.contains("speeds[next]"))
    }

    @Test
    fun languageRowRefreshesVisibleLabelImmediatelyAfterSavingPreference() {
        val source = settingsFragmentSource()

        assertLabelRefreshesAfterSave(source, "viewModel.setLanguage(langs[next])", "updateLanguageLabel(tvLanguage)")
    }

    private fun assertLabelRefreshesAfterSave(source: String, saveCall: String, labelUpdateCall: String) {
        val saveIndex = source.indexOf(saveCall)
        val labelIndex = source.indexOf(labelUpdateCall, saveIndex)

        assertTrue("settings row should save via $saveCall", saveIndex >= 0)
        assertTrue("settings row should refresh the displayed value via $labelUpdateCall", labelIndex > saveIndex)
    }

    private fun settingsFragmentSource(): String =
        rootFile(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "settings",
            "SettingsFragment.kt"
        ).readText()

    private fun playerGlassSheetDialogSource(): String =
        rootFile(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerGlassSheetDialog.kt"
        ).readText()

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
