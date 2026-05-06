package com.example.openvideo.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import com.example.openvideo.core.prefs.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

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
        assertEquals("zh", AppSettingsApplier.languageTagsFor("zh"))
        assertEquals("en", AppSettingsApplier.languageTagsFor("en"))
        assertEquals("", AppSettingsApplier.languageTagsFor("unsupported"))
    }
}
