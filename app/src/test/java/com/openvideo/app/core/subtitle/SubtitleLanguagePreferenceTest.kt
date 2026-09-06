package com.openvideo.app.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SubtitleLanguagePreferenceTest(private val key: String, private val language: SubtitleLanguage) {
    @Test
    fun normalizesBothLanguageKeysAndPreservesBilingualPreference() {
        for (bilingual in listOf(false, true)) {
            assertEquals(
                SubtitleLanguagePreference(language, language, bilingual),
                SubtitleLanguagePreference.fromKeys("  ${key.uppercase()}  ", key, bilingual)
            )
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "key={0}")
        fun cases(): List<Array<Any>> = listOf(
            arrayOf("chinese", SubtitleLanguage.CHINESE),
            arrayOf("english", SubtitleLanguage.ENGLISH),
            arrayOf("japanese", SubtitleLanguage.JAPANESE),
            arrayOf("korean", SubtitleLanguage.KOREAN),
            arrayOf("bilingual", SubtitleLanguage.BILINGUAL),
            arrayOf("unknown", SubtitleLanguage.UNKNOWN),
            arrayOf("", SubtitleLanguage.UNKNOWN)
        )
    }
}
