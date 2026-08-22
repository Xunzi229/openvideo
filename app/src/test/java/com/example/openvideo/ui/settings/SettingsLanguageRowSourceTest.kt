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

        assertLabelRefreshesAfterSave(source, "viewModel.setThemeMode(mode)", "updateThemeLabel(tvTheme)")
        assertLabelRefreshesAfterSave(source, "viewModel.setLanguage(lang)", "updateLanguageLabel(tvLanguage)")
        assertLabelRefreshesAfterSave(source, "viewModel.setDefaultRatio(option.ratio)", "updateRatioLabel(tvRatio)")
        assertLabelRefreshesAfterSave(source, "viewModel.setDefaultSpeed(speed)", "updateSpeedLabel(tvSpeed)")
    }

    @Test
    fun defaultPlaybackRowsOpenActionSheetPickersInsteadOfCyclingInline() {
        val source = settingsFragmentSource()

        assertTrue(source.contains("showDefaultRatioDialog(tvRatio)"))
        assertTrue(source.contains("showDefaultSpeedDialog(tvSpeed)"))
        assertTrue(source.contains("AppleActionSheet.show"))
        assertFalse(source.contains("PlayerGlassSheetDialog"))
        assertTrue(source.contains("DefaultPlayerSettings.supportedSpeeds"))
        assertTrue(source.contains("PlayerAspectRatioOptions.entries"))
        assertTrue(source.contains("viewModel.setDefaultRatio(option.ratio)"))
        assertTrue(source.contains("viewModel.setDefaultSpeed(speed)"))
        assertFalse(source.contains("ratios[next]"))
        assertFalse(source.contains("speeds[next]"))
        assertFalse(source.contains("modes[next]"))
        assertFalse(source.contains("langs[next]"))
        assertTrue(source.contains("showThemeSheet(tvTheme)"))
        assertTrue(source.contains("showLanguageSheet(tvLanguage, row)"))
    }

    @Test
    fun defaultRatioDialogUsesPlayerAspectOptionsForOrderAndLabels() {
        val source = settingsFragmentSource()
        val ratioBlock = source.substringAfter("private fun showDefaultRatioDialog(tvRatio: TextView)")
            .substringBefore("\n    private fun showDefaultSpeedDialog")

        assertTrue(source.contains("import com.example.openvideo.ui.player.PlayerAspectRatioOptions"))
        assertTrue(ratioBlock.contains("PlayerAspectRatioOptions.entries"))
        assertTrue(ratioBlock.contains("selected = option.ratio == viewModel.defaultRatio"))
        assertTrue(ratioBlock.contains("title = getString(option.labelRes)"))
        assertFalse(ratioBlock.contains("AspectRatio.entries"))
        assertFalse(source.contains("private fun ratioLabel("))
    }

    @Test
    fun settingsDialogsUseSingleActiveDialogGate() {
        val source = settingsFragmentSource()
        val ratioBlock = source.substringAfter("private fun showDefaultRatioDialog(tvRatio: TextView)")
            .substringBefore("\n    private fun showDefaultSpeedDialog")
        val speedBlock = source.substringAfter("private fun showDefaultSpeedDialog(tvSpeed: TextView)")
            .substringBefore("\n    private fun bindBackupSection")
        val themeBlock = source.substringAfter("private fun showThemeSheet(tvTheme: TextView)")
            .substringBefore("\n    private fun showLanguageSheet")
        val languageBlock = source.substringAfter("private fun showLanguageSheet(tvLanguage: TextView, row: View)")
            .substringBefore("\n    private fun showDefaultRatioDialog")

        assertTrue(source.contains("private var activeSettingsDialog: Dialog? = null"))
        assertTrue(source.contains("private fun showExclusiveSettingsDialog("))
        assertTrue(ratioBlock.contains("showExclusiveSettingsDialog"))
        assertTrue(speedBlock.contains("showExclusiveSettingsDialog"))
        assertTrue(themeBlock.contains("showExclusiveSettingsDialog"))
        assertTrue(languageBlock.contains("showExclusiveSettingsDialog"))
        assertTrue(ratioBlock.indexOf("showExclusiveSettingsDialog") < ratioBlock.indexOf("AppleActionSheet.show("))
        assertTrue(speedBlock.indexOf("showExclusiveSettingsDialog") < speedBlock.indexOf("AppleActionSheet.show("))
        assertTrue(themeBlock.indexOf("showExclusiveSettingsDialog") < themeBlock.indexOf("AppleActionSheet.show("))
        assertTrue(languageBlock.indexOf("showExclusiveSettingsDialog") < languageBlock.indexOf("AppleActionSheet.show("))
        assertTrue(source.contains("onDismiss = onDismiss"))
    }

    @Test
    fun defaultRatioAndSpeedDialogsUseActionSheetPickerChrome() {
        val source = settingsFragmentSource()
        val ratioBlock = source.substringAfter("private fun showDefaultRatioDialog(tvRatio: TextView)")
            .substringBefore("\n    private fun showDefaultSpeedDialog")
        val speedBlock = source.substringAfter("private fun showDefaultSpeedDialog(tvSpeed: TextView)")
            .substringBefore("\n    private fun bindBackupSection")

        assertTrue(ratioBlock.contains("AppleActionSheet.show("))
        assertTrue(speedBlock.contains("AppleActionSheet.show("))
        assertTrue(ratioBlock.contains("selected = option.ratio == viewModel.defaultRatio"))
        assertTrue(speedBlock.contains("selected = speed == viewModel.defaultSpeed"))
        assertFalse(source.contains("PlayerGlassSheetDialog"))
        assertFalse(source.contains("PlayerGlassSheetChoice"))
    }

    @Test
    fun languageRowRefreshesVisibleLabelImmediatelyAfterSavingPreference() {
        val source = settingsFragmentSource()

        assertLabelRefreshesAfterSave(source, "viewModel.setLanguage(lang)", "updateLanguageLabel(tvLanguage)")
    }

    @Test
    fun checkUpdateClickShowsHudThenAlertBeforeDownload() {
        val fragment = settingsFragmentSource()
        val viewModel = rootFile(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "settings",
            "SettingsViewModel.kt"
        ).readText()

        assertTrue(fragment.contains("viewModel.onCheckUpdateClick(requireActivity())"))
        assertTrue(viewModel.contains("AppleHud.show(activityContext, R.string.settings_update_checking)"))
        assertTrue(viewModel.contains("promptAvailableUpdate"))
        assertTrue(viewModel.contains("AppleAlertDialog.show"))
        assertTrue(viewModel.contains("R.string.settings_update_now"))
        assertTrue(viewModel.contains("downloadAndInstallUpdate"))
        assertTrue(viewModel.contains("settings_update_downloading"))
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
