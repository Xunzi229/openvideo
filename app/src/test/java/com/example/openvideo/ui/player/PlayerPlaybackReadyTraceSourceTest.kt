package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerPlaybackReadyTraceSourceTest {

    @Test
    fun playbackStateChangedUsesReadyTracePolicy() {
        val source = playerEventControllerSource()
        val block = source.substringAfter("override fun onPlaybackStateChanged(playbackState: Int) {")
            .substringBefore("\n            @OptIn(UnstableApi::class)")

        assertTrue(block.contains("PlayerPlaybackReadyTracePolicy.onPlaybackStateChanged("))
        assertTrue(block.contains("PlayerStartupTrace.Events.READY_AFTER_BUFFERING"))
        assertTrue(block.contains("ReadyTraceEvent.RECOVERED_AFTER_BUFFERING"))
        assertFalse(block.contains("if (playbackState == Player.STATE_BUFFERING)"))
    }

    @Test
    fun videoSwitchResetsBufferingLatch() {
        val source = playerActivitySource()
        assertTrue(source.contains("playbackWasBuffering = false"))
    }

    private fun playerActivitySource(): String {
        return kotlinSource("PlayerActivity.kt")
    }

    private fun playerEventControllerSource(): String {
        return kotlinSource("PlayerEventController.kt")
    }

    private fun kotlinSource(name: String): String {
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
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
