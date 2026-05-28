package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class IntroOutroSkipIntegrationTest {

    @Test
    fun playerActivityChecksIntroOutroSkipFromPositionUpdateLoop() {
        val source = String(Files.readAllBytes(playerPlaybackTickControllerSource()))

        assertTrue(source.contains("applyPlaybackTickSeek(state.currentPosition, state.duration)"))
        assertTrue(source.contains("PlayerPlaybackTickPolicy.seekTarget"))
        assertTrue(source.contains("hasSkippedOutro"))
        assertTrue(source.contains("playerPrefs.outroSeconds"))
    }

    private fun playerActivitySource(): Path {
        return kotlinSource("PlayerActivity.kt")
    }

    private fun playerPlaybackTickControllerSource(): Path {
        return kotlinSource("PlayerPlaybackTickController.kt")
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
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
