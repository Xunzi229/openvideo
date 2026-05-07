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

        assertTrue(source.contains("BottomSheetDialog"))
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
}
