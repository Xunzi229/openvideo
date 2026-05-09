package com.example.openvideo.core.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPrefsLanguageNormalizationTest {

    @Test
    fun normalizeMapsLegacyAndBcp47ToCanonicalKeys() {
        assertEquals("system", AppPrefs.normalizeLanguage("system"))
        assertEquals("system", AppPrefs.normalizeLanguage(""))
        assertEquals("system", AppPrefs.normalizeLanguage("unknown"))
        assertEquals("zh", AppPrefs.normalizeLanguage("zh"))
        assertEquals("zh", AppPrefs.normalizeLanguage("zh-CN"))
        assertEquals("zh", AppPrefs.normalizeLanguage("zh-rCN"))
        assertEquals("en", AppPrefs.normalizeLanguage("en"))
        assertEquals("en", AppPrefs.normalizeLanguage("en-US"))
    }
}
