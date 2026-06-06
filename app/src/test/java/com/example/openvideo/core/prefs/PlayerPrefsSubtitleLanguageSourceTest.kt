package com.example.openvideo.core.prefs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class PlayerPrefsSubtitleLanguageSourceTest {

    @Test
    fun playerPrefsPersistsSubtitleLanguagePreferenceKeys() {
        val source = String(
            Files.readAllBytes(
                sequenceOf(
                    Paths.get("src", "main", "java", "com", "example", "openvideo", "core", "prefs", "PlayerPrefs.kt"),
                    Paths.get("app", "src", "main", "java", "com", "example", "openvideo", "core", "prefs", "PlayerPrefs.kt")
                ).first(Files::exists)
            )
        )

        assertTrue(source.contains("var subtitlePrimaryLanguage"))
        assertTrue(source.contains("var subtitleSecondaryLanguage"))
        assertTrue(source.contains("var subtitlePreferBilingual"))
        assertTrue(source.contains("KEY_SUBTITLE_PRIMARY_LANGUAGE"))
        assertTrue(source.contains("KEY_SUBTITLE_SECONDARY_LANGUAGE"))
        assertTrue(source.contains("KEY_SUBTITLE_PREFER_BILINGUAL"))
    }
}
