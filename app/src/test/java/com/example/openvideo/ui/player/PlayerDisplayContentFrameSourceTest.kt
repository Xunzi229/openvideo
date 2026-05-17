package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerDisplayContentFrameSourceTest {

    @Test
    fun displaySettingsExposeContentFramePicker() {
        listOf(
            playerDisplaySettingsActivitySource(),
            playerDisplaySettingsSheetSource()
        ).forEach { path ->
            val source = String(Files.readAllBytes(path))
            assertTrue(source.contains("tv_content_frame_value"))
            assertTrue(source.contains("PlayerDisplayContentFrameControls.bind"))
        }
        val controlsSource = String(Files.readAllBytes(kotlinSource("PlayerDisplayContentFrameControls.kt")))
        assertTrue(controlsSource.contains("PlayerContentFrameSettingsPolicy.onModeSelected"))
    }

    @Test
    fun displayLayoutIncludesContentFrameRow() {
        val layout = String(Files.readAllBytes(displayLayout()))
        assertTrue(layout.contains("@+id/tv_content_frame_value"))
        assertTrue(layout.contains("@string/settings_content_frame"))
    }

    private fun playerDisplaySettingsActivitySource(): Path = kotlinSource("PlayerDisplaySettingsActivity.kt")

    private fun playerDisplaySettingsSheetSource(): Path = kotlinSource("PlayerDisplaySettingsSheet.kt")

    private fun displayLayout(): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", "activity_player_display_settings.xml")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun kotlinSource(name: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            name
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
