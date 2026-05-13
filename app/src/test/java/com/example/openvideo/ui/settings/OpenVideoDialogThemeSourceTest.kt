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
        assertTrue(theme.contains("buttonBarPositiveButtonStyle"))
        assertTrue(theme.contains("ShapeAppearance.OpenVideo.Dialog"))
    }

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
