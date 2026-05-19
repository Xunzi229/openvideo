package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerDecoderEventSourceTest {

    @Test
    fun playerActivityWiresDecoderInitializationThroughPolicy() {
        val source = playerActivitySource()
        assertTrue(
            "Activity must register a startup analytics listener after the player listener.",
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
        val source = playerActivitySource()
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
        val source = playerActivitySource()
        assertTrue(
            "onDestroy must remove the startup analytics listener.",
            source.contains("startupAnalyticsListener?.let { viewModel.player?.removeAnalyticsListener(it) }")
        )
        assertTrue(source.contains("startupAnalyticsListener = null"))
    }

    private fun playerActivitySource(): String {
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
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
