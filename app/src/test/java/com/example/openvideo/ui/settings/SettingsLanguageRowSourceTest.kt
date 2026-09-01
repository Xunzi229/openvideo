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

        assertLabelRefreshesAfterSave(source, "viewModel.setDefaultRatio(option.ratio)", "updateRatioLabel(tvRatio)")
        assertLabelRefreshesAfterSave(source, "viewModel.setDefaultSpeed(speed)", "updateSpeedLabel(tvSpeed)")
    }

    @Test
    fun configurationRowsUpdateOldViewsBeforeAppCompatCanRecreateTheHost() {
        val source = settingsFragmentSource()

        assertUpdatePrecedesApply(source, "updateThemeLabel(tvTheme, mode)", "viewModel.setThemeMode(mode)")
        assertUpdatePrecedesApply(source, "updateLanguageLabel(tvLanguage, lang)", "viewModel.setLanguage(lang)")
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
        assertTrue(source.contains("showLanguageSheet(tvLanguage)"))
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
        val languageBlock = source.substringAfter("private fun showLanguageSheet(tvLanguage: TextView)")
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

        val updateIndex = source.indexOf("updateLanguageLabel(tvLanguage, lang)")
        val saveIndex = source.indexOf("viewModel.setLanguage(lang)", updateIndex)
        assertTrue("language label must update before AppCompat may recreate the host", updateIndex >= 0)
        assertTrue("language preference must be applied after the old view is updated", saveIndex > updateIndex)
        assertFalse(source.contains("requireActivity().recreate()"))
        assertFalse(source.contains("activity?.recreate()"))
        assertFalse(source.contains("row.post"))
        assertTrue(source.contains("setApplicationLocales() owns Activity recreation"))
    }

    @Test
    fun importedConfigurationFeedbackDoesNotAttachAWindowToTheActivityBeingRecreated() {
        val source = settingsFragmentSource()
        val importBlock = source.substringAfter("private val importSettingsLauncher")
            .substringBefore("\n\n    companion object")

        assertTrue(importBlock.contains("val appContext = requireContext().applicationContext"))
        assertTrue(importBlock.contains("AppleHud.show(appContext, R.string.settings_toast_import_success)"))
        assertTrue(importBlock.indexOf("AppleHud.show(appContext") < importBlock.indexOf("applyImportedAppSettings()"))
        assertFalse(importBlock.contains("AppleHud.show(requireContext()"))
    }

    @Test
    fun checkUpdateClickShowsAlertThenOpensReleasePageInSystemBrowser() {
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
        assertTrue(viewModel.contains("R.string.settings_update_view_release"))
        assertTrue(viewModel.contains("ReleasePageLauncher.open(activityContext, release.releaseHtmlUrl)"))
        assertFalse(viewModel.contains("downloadAndInstallUpdate"))
        assertFalse(viewModel.contains("UpdateApkInstaller"))
        assertFalse(viewModel.contains("browserDownloadUrl"))
    }

    private fun assertLabelRefreshesAfterSave(source: String, saveCall: String, labelUpdateCall: String) {
        val saveIndex = source.indexOf(saveCall)
        val labelIndex = source.indexOf(labelUpdateCall, saveIndex)

        assertTrue("settings row should save via $saveCall", saveIndex >= 0)
        assertTrue("settings row should refresh the displayed value via $labelUpdateCall", labelIndex > saveIndex)
    }

    private fun assertUpdatePrecedesApply(source: String, updateCall: String, applyCall: String) {
        val updateIndex = source.indexOf(updateCall)
        val applyIndex = source.indexOf(applyCall, updateIndex)

        assertTrue("settings row should update via $updateCall", updateIndex >= 0)
        assertTrue("$updateCall must run before $applyCall", applyIndex > updateIndex)
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
