package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
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
}
