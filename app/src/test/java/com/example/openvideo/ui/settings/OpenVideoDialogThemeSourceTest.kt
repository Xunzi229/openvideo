package com.example.openvideo.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class OpenVideoDialogThemeSourceTest {

    @Test
    fun appThemeUsesOpenVideoMaterialAlertDialogOverlay() {
        val lightTheme = rootFile("app", "src", "main", "res", "values", "themes.xml").readText()
        val darkTheme = rootFile("app", "src", "main", "res", "values-night", "themes.xml").readText()

        assertTrue(lightTheme.contains("materialAlertDialogTheme"))
        assertTrue(lightTheme.contains("ThemeOverlay.OpenVideo.MaterialAlertDialog"))
        assertTrue(darkTheme.contains("materialAlertDialogTheme"))
        assertTrue(darkTheme.contains("ThemeOverlay.OpenVideo.MaterialAlertDialog"))
    }

    @Test
    fun dialogOverlayUsesThemeTokensForSurfaceTextShapeAndButtons() {
        val lightTokens = rootFile("app", "src", "main", "res", "values", "design_tokens.xml").readText()
        val darkTokens = rootFile("app", "src", "main", "res", "values-night", "design_tokens.xml").readText()
        val theme = rootFile("app", "src", "main", "res", "values", "themes.xml").readText()

        assertTrue(lightTokens.contains("ov_dialog_surface"))
        assertTrue(lightTokens.contains("ov_dialog_scrim"))
        assertTrue(darkTokens.contains("ov_dialog_surface"))
        assertTrue(darkTokens.contains("ov_dialog_scrim"))
        assertTrue(theme.contains("materialAlertDialogTitleTextStyle"))
        assertTrue(theme.contains("materialAlertDialogBodyTextStyle"))
        assertTrue(theme.contains("<style name=\"TextAppearance.OpenVideo.Dialog.Title\" parent=\"MaterialAlertDialog.Material3.Title.Text\">"))
        assertTrue(theme.contains("<style name=\"TextAppearance.OpenVideo.Dialog.Body\" parent=\"MaterialAlertDialog.Material3.Body.Text\">"))
        assertTrue(theme.contains("buttonBarPositiveButtonStyle"))
        assertTrue(theme.contains("buttonBarNegativeButtonStyle"))
        assertTrue(theme.contains("<item name=\"android:layout_width\">wrap_content</item>"))
        assertTrue(theme.contains("<item name=\"android:layout_height\">48dp</item>"))
        assertTrue(theme.contains("ShapeAppearance.OpenVideo.Dialog"))
    }

    @Test
    fun playerGlassSheetUsesThemeSpecificColorTokens() {
        val lightTokens = rootFile("app", "src", "main", "res", "values", "aspect_ratio_dialog_tokens.xml").readText()
        val darkTokens = rootFile("app", "src", "main", "res", "values-night", "aspect_ratio_dialog_tokens.xml").readText()
        val layout = rootFile("app", "src", "main", "res", "layout", "dialog_player_glass_sheet.xml").readText()

        assertTrue(lightTokens.contains("<color name=\"player_aspect_dialog_panel_bg\">#CCFFFFFF</color>"))
        assertTrue(lightTokens.contains("<color name=\"player_glass_sheet_title\">#FF172033</color>"))
        assertTrue(lightTokens.contains("<color name=\"player_aspect_row_bg\">#80FFFFFF</color>"))
        assertTrue(lightTokens.contains("<color name=\"player_aspect_row_label_normal\">#DE172033</color>"))
        assertTrue(darkTokens.contains("<color name=\"player_aspect_dialog_panel_bg\">#A60C101C</color>"))
        assertTrue(darkTokens.contains("<color name=\"player_glass_sheet_title\">#FFFFFFFF</color>"))
        assertTrue(layout.contains("android:textColor=\"@color/player_glass_sheet_title\""))
    }

    @Test
    fun playerGlassSheetAppliesBackdropBlurForFrostedGlass() {
        val source = rootFile(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerGlassSheetDialog.kt"
        ).readText()

        assertTrue(source.contains("setBackgroundBlurRadius"))
        assertTrue(source.contains("18f * dm.density"))
        assertTrue(source.contains("dimAmount = 0.48f"))
    }

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
