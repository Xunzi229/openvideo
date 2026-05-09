package com.example.openvideo.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.openvideo.core.prefs.AppPrefs
import com.example.openvideo.core.prefs.ThemeMode

object AppSettingsApplier {

    fun apply(appPrefs: AppPrefs) {
        AppCompatDelegate.setDefaultNightMode(nightModeFor(appPrefs.themeMode))
        when (appPrefs.language) {
            "zh" -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))
            "en" -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            else -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }

    fun nightModeFor(themeMode: ThemeMode): Int {
        return when (themeMode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    /** BCP-47 tags for tests and logging; empty means follow system. */
    fun languageTagsFor(language: String): String {
        return when (language) {
            "zh" -> "zh-CN"
            "en" -> "en"
            else -> ""
        }
    }
}
