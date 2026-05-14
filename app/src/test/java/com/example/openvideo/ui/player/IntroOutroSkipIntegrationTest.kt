package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class IntroOutroSkipIntegrationTest {

    @Test
    fun playerActivityChecksIntroOutroSkipFromPositionUpdateLoop() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(source.contains("applyPlaybackTickSeek(state.currentPosition, state.duration)"))
        assertTrue(source.contains("PlayerPlaybackTickPolicy.seekTarget"))
        assertTrue(source.contains("hasSkippedOutro"))
        assertTrue(source.contains("playerPrefs.outroSeconds"))
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
