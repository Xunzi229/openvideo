package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerPlaylistLoopSourceTest {

    @Test
    fun playerActivityHandlesEndedStateWithSessionQueuePolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val listener = source
            .substringAfter("playerListener = object : Player.Listener")
            .substringBefore("viewModel.player?.addListener")

        assertTrue(listener.contains("Player.STATE_ENDED"))
        assertTrue(listener.contains("playNextQueueVideoAfterEnded()"))

        val endedHandler = source
            .substringAfter("private fun playNextQueueVideoAfterEnded()")
            .substringBefore("\n    private fun", missingDelimiterValue = "")

        assertTrue(endedHandler.contains("PlayerQueueLoopPolicy.nextIndexAfterEnded"))
        assertTrue(endedHandler.contains("viewModel.sessionQueue.value"))
        assertTrue(endedHandler.contains("viewModel.playingVideoId"))
        assertTrue(endedHandler.contains("loopMode = playerPrefs.loopMode"))
        assertTrue(endedHandler.contains("viewModel.switchToVideo(queue[nextIndex])"))
        assertTrue(endedHandler.contains("applyPlayerSettings()"))
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
