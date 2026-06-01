package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerDecoderEventSourceTest {

    @Test
    fun playerActivityWiresDecoderInitializationThroughPolicy() {
        val activitySource = playerActivitySource()
        val source = playerEventControllerSource()
        assertTrue(
            "Activity must register a startup analytics listener after the player listener.",
            activitySource.contains("playerEvents.attach()") &&
                source.contains("attachStartupAnalyticsListener()")
        )
        assertTrue(
            "onVideoDecoderInitialized must go through PlayerDecoderEventPolicy.",
            source.contains("PlayerDecoderEventPolicy.videoDecoderEvents(decoderName)")
        )
        assertTrue(
            "onAudioDecoderInitialized must go through PlayerDecoderEventPolicy.",
            source.contains("PlayerDecoderEventPolicy.audioDecoderEvents(decoderName)")
        )
        assertTrue(
            "Decoder events must be recorded once each via PlayerStartupTrace.recordOnce.",
            source.contains(".forEach { startupTrace.recordOnce(it) }")
        )
    }

    @Test
    fun playerActivityWiresCodecErrorsThroughPolicy() {
        val source = playerEventControllerSource()
        assertTrue(
            "onVideoCodecError must go through PlayerDecoderEventPolicy.",
            source.contains("PlayerDecoderEventPolicy.videoCodecErrorEvents(videoCodecError.javaClass.name)")
        )
        assertTrue(
            "onAudioCodecError must go through PlayerDecoderEventPolicy.",
            source.contains("PlayerDecoderEventPolicy.audioCodecErrorEvents(audioCodecError.javaClass.name)")
        )
    }

    @Test
    fun playerActivityRemovesAnalyticsListenerOnDestroy() {
        val activitySource = playerActivitySource()
        val source = playerEventControllerSource()
        assertTrue(
            "onDestroy must remove the startup analytics listener.",
            activitySource.contains("playerEvents.detach()") &&
                source.contains("startupAnalyticsListener?.let { viewModel.player?.removeAnalyticsListener(it) }")
        )
        assertTrue(source.contains("startupAnalyticsListener = null"))
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
