package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSubtitleSettingsSheetTest {

    @Test
    fun sheetDoesNotShowLoadedToastWhenSubtitlePickerReturns() {
        val source = String(Files.readAllBytes(playerSubtitleSettingsSheetSource()))

        assertFalse(source.contains("settings_subtitle_loaded"))
        assertFalse(source.contains("player_subtitle_loaded"))
    }

    @Test
    fun subtitleSettingsLayoutIncludesPreviewAndDelayControls() {
        val layout = String(Files.readAllBytes(playerSubtitleSettingsLayout()))

        assertTrue(layout.contains("@+id/tv_subtitle_preview"))
        assertTrue(layout.contains("@+id/tv_subtitle_delay_value"))
        assertTrue(layout.contains("@+id/btn_subtitle_delay_minus"))
        assertTrue(layout.contains("@+id/btn_subtitle_delay_plus"))
        assertTrue(layout.contains("@+id/btn_subtitle_delay_reset"))
    }

    @Test
    fun subtitleSettingsSheetAndActivityRefreshPreviewAndDelayValue() {
        listOf(
            playerSubtitleSettingsSheetSource(),
            playerSubtitleSettingsActivitySource()
        ).forEach { sourcePath ->
            val source = String(Files.readAllBytes(sourcePath))
            assertTrue(source.contains("tv_subtitle_preview"))
            assertTrue(source.contains("tv_subtitle_delay_value"))
            assertTrue(source.contains("updateSubtitlePreview()"))
            assertTrue(source.contains("PlayerSubtitleSettingsPreviewPolicy.apply"))
            assertTrue(source.contains("updateSubtitleDelayText()"))
            assertTrue(source.contains("playerPrefs.subtitleDelayMs"))
        }
    }

    private fun playerSubtitleSettingsSheetSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerSubtitleSettingsSheet.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerSubtitleSettingsActivitySource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerSubtitleSettingsActivity.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerSubtitleSettingsLayout(): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", "activity_player_subtitle_settings.xml")
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
