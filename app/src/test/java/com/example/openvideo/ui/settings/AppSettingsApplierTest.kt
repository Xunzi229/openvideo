package com.example.openvideo.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import com.example.openvideo.core.prefs.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class AppSettingsApplierTest {

    @Test
    fun mapsThemeModeToAppCompatNightMode() {
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppSettingsApplier.nightModeFor(ThemeMode.SYSTEM)
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            AppSettingsApplier.nightModeFor(ThemeMode.DARK)
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppSettingsApplier.nightModeFor(ThemeMode.LIGHT)
        )
    }

    @Test
    fun mapsLanguageToApplicationLocaleTags() {
        assertEquals("", AppSettingsApplier.languageTagsFor("system"))
        assertEquals("zh-CN", AppSettingsApplier.languageTagsFor("zh"))
        assertEquals("en", AppSettingsApplier.languageTagsFor("en"))
        assertEquals("", AppSettingsApplier.languageTagsFor("unsupported"))
    }

    @Test
    fun settingsViewModelAppliesOnlyTheConfigurationThatChanged() {
        val path = sequenceOf(
            Paths.get("app/src/main/java/com/example/openvideo/ui/settings/SettingsViewModel.kt"),
            Paths.get("src/main/java/com/example/openvideo/ui/settings/SettingsViewModel.kt")
        ).first(Files::exists)
        val source = String(Files.readAllBytes(path))
        val themeBlock = source.substringAfter("fun setThemeMode(").substringBefore("\n    fun setLanguage")
        val languageBlock = source.substringAfter("fun setLanguage(").substringBefore("\n    fun applyImportedAppSettings")

        assertEquals(true, themeBlock.contains("AppSettingsApplier.applyTheme(mode)"))
        assertEquals(false, themeBlock.contains("AppSettingsApplier.apply(appPrefs)"))
        assertEquals(true, languageBlock.contains("AppSettingsApplier.applyLanguage(appPrefs.language)"))
        assertEquals(false, languageBlock.contains("AppSettingsApplier.apply(appPrefs)"))
    }
}
