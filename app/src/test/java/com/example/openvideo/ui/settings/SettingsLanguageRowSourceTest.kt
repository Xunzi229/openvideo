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
        assertTrue(source.contains("PlayerAspectRatioOptions.entries"))
        assertTrue(source.contains("viewModel.setDefaultRatio(ratio)"))
        assertTrue(source.contains("viewModel.setDefaultSpeed(speed)"))
        assertFalse(source.contains("ratios[next]"))
        assertFalse(source.contains("speeds[next]"))
    }

    @Test
    fun defaultRatioDialogUsesPlayerAspectOptionsForOrderAndLabels() {
        val source = settingsFragmentSource()
        val ratioBlock = source.substringAfter("private fun showDefaultRatioDialog(tvRatio: TextView)")
            .substringBefore("\n    private fun showDefaultSpeedDialog")

        assertTrue(source.contains("import com.example.openvideo.ui.player.PlayerAspectRatioOptions"))
        assertTrue(ratioBlock.contains("PlayerAspectRatioOptions.entries"))
        assertTrue(ratioBlock.contains("value = option.ratio"))
        assertTrue(ratioBlock.contains("label = getString(option.labelRes)"))
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

        assertTrue(source.contains("private var activeSettingsDialog: Dialog? = null"))
        assertTrue(source.contains("private fun showExclusiveSettingsDialog("))
        assertTrue(ratioBlock.contains("showExclusiveSettingsDialog"))
        assertTrue(speedBlock.contains("showExclusiveSettingsDialog"))
        assertTrue(ratioBlock.indexOf("showExclusiveSettingsDialog") < ratioBlock.indexOf("PlayerGlassSheetDialog.showSingleChoice("))
        assertTrue(speedBlock.indexOf("showExclusiveSettingsDialog") < speedBlock.indexOf("PlayerGlassSheetDialog.showSingleChoice("))
        assertTrue(source.contains("onDismiss = onDismiss"))
    }

    @Test
    fun defaultRatioAndSpeedDialogsUseSameCenterAnimationChrome() {
        val source = settingsFragmentSource()
        val glassDialogSource = playerGlassSheetDialogSource()
        val themeSource = rootFile("app", "src", "main", "res", "values", "themes.xml").readText()
        val ratioBlock = source.substringAfter("private fun showDefaultRatioDialog(tvRatio: TextView)")
            .substringBefore("\n    private fun showDefaultSpeedDialog")
        val speedBlock = source.substringAfter("private fun showDefaultSpeedDialog(tvSpeed: TextView)")
            .substringBefore("\n    private fun bindBackupSection")

        assertFalse("Default ratio dialog should use the shared CENTER chrome.", ratioBlock.contains("chrome ="))
        assertFalse("Default speed dialog should use the shared CENTER chrome.", speedBlock.contains("chrome ="))
        assertTrue(glassDialogSource.contains("windowAnimations = R.style.Animation_OpenVideo_CenterDialog"))
        assertTrue(themeSource.contains("<style name=\"Animation.OpenVideo.CenterDialog\""))
        assertTrue(themeSource.contains("@anim/dialog_center_in"))
        assertTrue(themeSource.contains("@anim/dialog_center_out"))
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
