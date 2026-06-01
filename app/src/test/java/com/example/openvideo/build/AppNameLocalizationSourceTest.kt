package com.example.openvideo.build

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AppNameLocalizationSourceTest {

    @Test
    fun englishAppNameStaysOpenVideoAndChineseAppNameUsesQingYing() {
        val english = rootFile("app", "src", "main", "res", "values", "strings.xml").readText()
        val chinese = rootFile("app", "src", "main", "res", "values-zh-rCN", "strings.xml").readText()

        assertTrue(english.contains("""<string name="app_name">OpenVideo</string>"""))
        assertTrue(chinese.contains("""<string name="app_name">清影</string>"""))
    }

    @Test
    fun chineseStringsDoNotUseOpenVideoAsUserFacingAppName() {
        val chinese = rootFile("app", "src", "main", "res", "values-zh-rCN", "strings.xml").readText()

        assertFalse(chinese.contains(">OpenVideo<"))
        assertFalse(chinese.contains("OpenVideo "))
        assertFalse(chinese.contains(" OpenVideo"))
        assertTrue(chinese.contains("清影"))
    }

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
