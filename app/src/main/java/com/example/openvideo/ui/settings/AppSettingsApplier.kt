package com.example.openvideo.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.openvideo.core.prefs.AppPrefs
import com.example.openvideo.core.prefs.ThemeMode

object AppSettingsApplier {

    fun apply(appPrefs: AppPrefs) {
        applyTheme(appPrefs.themeMode)
        applyLanguage(appPrefs.language)
    }

    fun applyTheme(themeMode: ThemeMode) {
        val targetMode = nightModeFor(themeMode)
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode)
        }
    }

    fun applyLanguage(language: String) {
        val targetTags = languageTagsFor(language)
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != targetTags) {
            val locales = if (targetTags.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(targetTags)
            }
            AppCompatDelegate.setApplicationLocales(locales)
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
