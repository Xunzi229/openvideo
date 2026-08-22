package com.example.openvideo.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AppleStyleAndSizeSourceTest {

    @Test
    fun lightTokensUseAppleGroupedSurfaceAndSystemBlue() {
        val tokens = rootFile("app", "src", "main", "res", "values", "design_tokens.xml").readText()

        assertTrue(tokens.contains("#FFF2F2F7"))
        assertTrue(tokens.contains("#FF007AFF"))
        assertTrue(tokens.contains("ov_text_large_title"))
        assertTrue(tokens.contains("ov_row_height"))
        assertTrue(tokens.contains("ov_icon_button_size"))
        assertTrue(tokens.contains("<dimen name=\"ov_row_height\">56dp</dimen>"))
    }

    @Test
    fun sizeTokensScaleBySmallestWidthAndShortHeight() {
        val sw360 = rootFile("app", "src", "main", "res", "values-sw360dp", "design_tokens.xml").readText()
        val sw600 = rootFile("app", "src", "main", "res", "values-sw600dp", "design_tokens.xml").readText()
        val sw840 = rootFile("app", "src", "main", "res", "values-sw840dp", "design_tokens.xml").readText()
        val land = rootFile("app", "src", "main", "res", "values-land", "design_tokens.xml").readText()
        val sw360Land = rootFile("app", "src", "main", "res", "values-sw360dp-land", "design_tokens.xml").readText()

        assertTrue(sw360.contains("ov_space_page"))
        assertTrue(sw600.contains("ov_space_page"))
        assertTrue(sw840.contains("ov_space_page"))
        assertTrue(land.contains("<dimen name=\"ov_bottom_nav_height\">64dp</dimen>"))
        assertTrue(sw360Land.contains("<dimen name=\"ov_bottom_nav_height\">64dp</dimen>"))
    }

    @Test
    fun mainActivityAppliesSystemBarAndCutoutInsets() {
        val source = rootFile(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "MainActivity.kt"
        ).readText()
        val layout = rootFile("app", "src", "main", "res", "layout", "activity_main.xml").readText()

        assertTrue(source.contains("WindowCompat.setDecorFitsSystemWindows(window, false)"))
        assertTrue(source.contains("SystemBarInsetsPolicy.union("))
        assertTrue(source.contains("WindowInsetsCompat.Type.displayCutout()"))
        assertTrue(layout.contains("""android:id="@+id/main_root""""))
        assertTrue(layout.contains("@drawable/bg_tab_bar"))
        assertTrue(layout.contains("app:itemPaddingTop"))
        assertTrue(layout.contains("app:itemPaddingBottom"))
        assertTrue(layout.contains("@dimen/ov_bottom_nav_height"))
        val tokens = rootFile("app", "src", "main", "res", "values", "design_tokens.xml").readText()
        assertTrue(tokens.contains("<dimen name=\"ov_bottom_nav_height\">64dp</dimen>"))
        assertFalse(tokens.contains("<dimen name=\"ov_bottom_nav_height\">49dp</dimen>"))
        assertFalse(source.contains("gh release create"))
    }

    @Test
    fun settingsAndSourcesUseGroupedInsetCards() {
        val settings = rootFile("app", "src", "main", "res", "layout", "fragment_settings.xml").readText()
        val theme = rootFile("app", "src", "main", "res", "values", "themes.xml").readText()

        assertTrue(theme.contains("Widget.OpenVideo.GroupedSection"))
        assertTrue(theme.contains("Widget.OpenVideo.Chevron"))
        assertTrue(theme.contains("@drawable/ic_chevron_right"))
        assertTrue(settings.contains("@style/Widget.OpenVideo.GroupedSection"))
        assertTrue(settings.contains("@style/Widget.OpenVideo.Chevron"))
    }

    private fun Path.readText(): String = String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
