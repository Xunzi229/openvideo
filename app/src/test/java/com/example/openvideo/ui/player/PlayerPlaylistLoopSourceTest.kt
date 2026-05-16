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
        assertTrue(listener.contains("handlePlaybackEnded()"))

        val endedHandler = source
            .substringAfter("private fun handlePlaybackEnded()")
            .substringBefore("\n    private fun", missingDelimiterValue = "")

        assertTrue(endedHandler.contains("PlayerPlaybackEndPolicy.decide"))
        assertTrue(endedHandler.contains("viewModel.sessionQueue.value"))
        assertTrue(endedHandler.contains("viewModel.playingVideoId"))
        assertTrue(endedHandler.contains("loopMode = playerPrefs.loopMode"))
        assertTrue(endedHandler.contains("PlayerPlaybackEndAction.PLAY_NEXT"))
        assertTrue(endedHandler.contains("PlayerPlaybackEndAction.REPLAY_CURRENT"))
        assertTrue(endedHandler.contains("PlayerPlaybackEndAction.RETURN_TO_LIST"))
        assertTrue(endedHandler.contains("playNextQueueVideoAfterEnded(queue, decision.nextIndex)"))

        val nextHandler = source
            .substringAfter("private fun playNextQueueVideoAfterEnded(queue: List<VideoItem>, nextIndex: Int?)")
            .substringBefore("\n    private fun", missingDelimiterValue = "")

        assertTrue(nextHandler.contains("viewModel.switchToVideo(queue[nextIndex])"))
        assertTrue(nextHandler.contains("applyPlayerSettings()"))
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
