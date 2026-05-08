package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSettingsDialogTest {

    @Test
    fun playerSettingsUseImmersiveInPlayerSheetInsteadOfSettingsPage() {
        val source = String(Files.readAllBytes(playerSettingsDialogSource()))

        assertTrue(source.contains(": Dialog(context)"))
        assertFalse(source.contains(": BottomSheetDialog(context)"))
        assertTrue(source.contains("setCanceledOnTouchOutside(true)"))
        assertTrue(source.contains("setupPrimaryGrid()"))
        assertTrue(source.contains("showDetailPage("))
        assertTrue(source.contains("showPrimaryPage()"))
        assertTrue(source.contains("PlayerSettingsLayoutPolicy.panelBounds"))
        assertTrue(source.contains("PlayerSettingsLayoutPolicy.panelGravity"))

        assertFalse(source.contains("PlayerDisplaySettingsActivity"))
        assertFalse(source.contains("PlayerPlaybackSettingsActivity"))
        assertFalse(source.contains("BaseSettingsSheet"))
    }

    @Test
    fun everyPrimarySettingsFeatureHasARealActionOrPage() {
        val source = String(Files.readAllBytes(playerSettingsDialogSource()))

        assertFalse(source.contains("showUnavailable"))
        assertFalse(source.contains("buildUnavailablePage"))
        assertFalse(source.contains("player_sheet_not_available"))

        listOf(
            "buildAudioPage()",
            "buildSubtitlePage()",
            "buildAspectPage()",
            "buildDisplayPage()",
            "buildPlaylistPage()",
            "buildStreamPage()",
            "buildInfoPage()",
            "shareVideoTitle()",
            "buildCutPage()",
            "buildBookmarkPage()",
            "buildTutorialPage()",
            "buildMorePage()"
        ).forEach { expected ->
            assertTrue("Missing real settings handler: $expected", source.contains(expected))
        }
    }

    @Test
    fun subtitleDelayAndNetworkStreamAreWiredToPlayback() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val viewModelSource = String(Files.readAllBytes(playerViewModelSource()))

        assertTrue(dialogSource.contains("playerPrefs.subtitleDelayMs = value"))
        assertTrue(viewModelSource.contains("+ playerPrefs.subtitleDelayMs"))
        assertTrue(dialogSource.contains("viewModel.playStream("))
    }

    @Test
    fun playlistAndShareActionsAreWiredToRealPlaybackData() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val viewModelSource = String(Files.readAllBytes(playerViewModelSource()))

        assertTrue(dialogSource.contains("viewModel.addCurrentVideoToDefaultPlaylist()"))
        assertTrue(viewModelSource.contains("repository.addToQuickPlaylist("))
        assertTrue(dialogSource.contains("viewModel.currentVideoShareText()"))
    }

    @Test
    fun playerSettingsLayoutHasGridAndDetailPages() {
        val layout = String(Files.readAllBytes(playerSettingsLayout()))

        assertTrue(layout.contains("@+id/settings_grid"))
        assertTrue(layout.contains("android:columnCount=\"4\""))
        assertTrue(layout.contains("@+id/settings_primary_page"))
        assertTrue(layout.contains("@+id/settings_detail_page"))
        assertTrue(layout.contains("@+id/settings_detail_back"))
        assertTrue(layout.contains("@+id/settings_detail_container"))
        assertTrue(layout.contains("@drawable/bg_player_settings_sheet"))
        assertFalse(layout.contains("@+id/nav_playback"))
    }

    @Test
    fun playerSettingsSheetUsesTransparentGlassOverVideo() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val portraitSheet = String(Files.readAllBytes(playerSettingsSheetDrawable()))
        val landscapeSheet = String(Files.readAllBytes(playerSettingsLandscapeSheetDrawable()))

        listOf(portraitSheet, landscapeSheet).forEach { sheet ->
            assertFalse(
                "Settings sheet should not use the old near-opaque black background.",
                sheet.contains("#F2000000", ignoreCase = true)
            )
            assertTrue("Glass sheet should use a translucent gradient.", sheet.contains("<gradient"))
            assertTrue("Glass sheet should keep a subtle glass edge.", sheet.contains("<stroke"))
            assertTrue(
                "Glass sheet should include translucent colors so the video remains visible.",
                listOf("#66", "#73", "#80", "#8F", "#99", "#A6", "#AA", "#B3").any {
                    sheet.contains(it, ignoreCase = true)
                }
            )
        }

        assertTrue(
            "Android 12+ devices should blur the playing video behind the sheet.",
            dialogSource.contains("setBackgroundBlurRadius(dp(18))")
                || dialogSource.contains("setBackgroundBlurRadius(dp(20))")
        )
        assertTrue(
            "The dim overlay should be light enough to preserve the playing video.",
            dialogSource.contains("setDimAmount(0.18f)")
        )
    }

    @Test
    fun playerSettingsSheetLooksLikeAFloatingRightGlassPanel() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val layout = String(Files.readAllBytes(playerSettingsLayout()))
        val landscapeSheet = String(Files.readAllBytes(playerSettingsLandscapeSheetDrawable()))

        assertTrue(dialogSource.contains("PlayerSettingsLayoutPolicy.landscapeMarginPx("))
        assertTrue(dialogSource.contains("attributes = attributes.apply"))
        assertTrue(dialogSource.contains("x = PlayerSettingsLayoutPolicy.landscapeMarginPx"))
        assertTrue(dialogSource.contains("decorView.elevation = dp(20).toFloat()"))

        assertTrue(layout.contains("android:elevation=\"20dp\""))
        assertTrue(layout.contains("android:scrollbarThumbVertical=\"@drawable/bg_player_settings_scrollbar_thumb\""))
        assertTrue(layout.contains("@+id/settings_primary_scroll"))
        assertTrue(layout.contains("@+id/settings_detail_scroll"))
        assertTrue(layout.contains("@drawable/bg_player_settings_bottom_fade"))

        assertTrue("Landscape glass panel should have large rounded corners.", landscapeSheet.contains("android:radius=\"28dp\""))
        assertTrue("Landscape glass panel should be highly translucent.", landscapeSheet.contains("#7312161E"))
        assertTrue("Landscape panel needs a soft glass edge.", landscapeSheet.contains("#2EFFFFFF"))
    }

    @Test
    fun playerSettingsGridUsesCompactSpacing() {
        val dialogSource = String(Files.readAllBytes(playerSettingsDialogSource()))
        val layout = String(Files.readAllBytes(playerSettingsLayout()))

        assertTrue(layout.contains("android:paddingStart=\"12dp\""))
        assertTrue(layout.contains("android:paddingEnd=\"12dp\""))
        assertTrue(dialogSource.contains("minimumHeight = dp(74)"))
        assertTrue(dialogSource.contains("setPadding(1, 4, 1, 2)"))
        assertTrue(dialogSource.contains("setMargins(dp(1), dp(2), dp(1), dp(2))"))
    }

    private fun playerSettingsDialogSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerSettingsDialog.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerSettingsLayout(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            "layout",
            "dialog_player_settings.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerViewModelSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerViewModel.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerSettingsSheetDrawable(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            "drawable",
            "bg_player_settings_sheet.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerSettingsLandscapeSheetDrawable(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            "drawable-land",
            "bg_player_settings_sheet.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
