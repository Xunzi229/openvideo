package com.example.openvideo.ui.settings

import org.junit.Assert.assertTrue
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
        assertLabelRefreshesAfterSave(source, "viewModel.setDefaultRatio(ratios[next])", "updateRatioLabel(tvRatio)")
        assertLabelRefreshesAfterSave(source, "viewModel.setDefaultSpeed(speeds[next])", "updateSpeedLabel(tvSpeed)")
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

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
