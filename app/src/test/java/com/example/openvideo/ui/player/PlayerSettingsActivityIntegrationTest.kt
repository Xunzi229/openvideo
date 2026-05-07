package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSettingsActivityIntegrationTest {

    @Test
    fun settingsSheetWeakensControlsAndImmediatePreferencesAffectPlaybackChrome() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(source.contains("btnSettings.setOnClickListener"))
        assertTrue(source.contains("hideControls()"))
        assertTrue(source.contains("if (playerPrefs.subtitlesEnabled) viewModel.getCurrentSubtitle() else \"\""))
        assertTrue(source.contains("playerView.alpha = if (playerPrefs.videoDisplayEnabled) 1f else 0f"))
        assertTrue(source.contains("bottomPanel.alpha = playerPrefs.controlsOpacity / 100f"))
        assertTrue(source.contains("playerManager.applyVideoAdjustments("))
    }

    private fun playerActivitySource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerActivity.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
