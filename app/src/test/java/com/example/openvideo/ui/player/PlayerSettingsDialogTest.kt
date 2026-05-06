package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSettingsDialogTest {

    @Test
    fun mxStyleSettingsStayInSinglePanelAndDoNotLaunchNestedSheets() {
        val source = String(Files.readAllBytes(playerSettingsDialogSource()))

        assertTrue(source.contains("showChoiceDialog("))
        assertTrue(source.contains("bindValueChoice("))
        assertTrue(source.contains("private fun bindSeekRow"))
        assertTrue(source.contains("setupSpeedRow()"))
        assertTrue(source.contains("setupPlaybackSection()"))
        assertTrue(source.contains("setupVideoSection()"))
        assertTrue(source.contains("setupAudioSection()"))
        assertTrue(source.contains("setupSubtitleSection()"))
        assertTrue(source.contains("setupGestureSection()"))

        assertFalse(source.contains("showDetailSettingsSheet"))
        assertFalse(source.contains("bindDetailLauncher"))
        assertFalse(source.contains("BaseSettingsSheet"))
        assertFalse(source.contains("row(R.id.row_") && source.contains("visibility = View.GONE"))
        assertFalse(source.contains("showDetailSettingsForNav"))
    }

    @Test
    fun mxStyleLayoutUsesValueRowsForTapToConfigureSettings() {
        val layout = String(Files.readAllBytes(playerSettingsLayout()))

        assertTrue(layout.contains("@+id/row_speed"))
        assertTrue(layout.contains("@+id/row_loop"))
        assertTrue(layout.contains("@+id/row_aspect"))
        assertTrue(layout.contains("@+id/row_channel"))
        assertTrue(layout.contains("@+id/row_subtitle_bg"))
        assertFalse(layout.contains("@+id/rg_speed"))
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
}
