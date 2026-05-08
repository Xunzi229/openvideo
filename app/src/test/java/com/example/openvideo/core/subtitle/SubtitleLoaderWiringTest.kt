package com.example.openvideo.core.subtitle

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class SubtitleLoaderWiringTest {

    @Test
    fun playerSubtitleEncodingPreferenceIsUsedWhenLoadingFiles() {
        val source = String(
            Files.readAllBytes(
                sequenceOf(
                    Paths.get(
                        "src",
                        "main",
                        "java",
                        "com",
                        "example",
                        "openvideo",
                        "core",
                        "subtitle",
                        "SubtitleLoader.kt"
                    ),
                    Paths.get(
                        "app",
                        "src",
                        "main",
                        "java",
                        "com",
                        "example",
                        "openvideo",
                        "core",
                        "subtitle",
                        "SubtitleLoader.kt"
                    )
                ).first(Files::exists)
            )
        )

        assertTrue(source.contains("private val playerPrefs: PlayerPrefs"))
        assertTrue(source.contains("playerPrefs.subtitleEncoding"))
        assertTrue(source.contains("charsetForPreference("))
    }
}
