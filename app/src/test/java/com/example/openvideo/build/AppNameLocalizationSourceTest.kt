package com.example.openvideo.build

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AppNameLocalizationSourceTest {

    @Test
    fun appNameStaysOpenVideoInEnglishAndChinese() {
        val english = rootFile("app", "src", "main", "res", "values", "strings.xml").readText()
        val chinese = rootFile("app", "src", "main", "res", "values-zh-rCN", "strings.xml").readText()

        val expected = """<string name="app_name">Open Video</string>"""
        assertTrue(english.contains(expected))
        assertTrue(chinese.contains(expected))
    }

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
