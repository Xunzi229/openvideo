package com.example.openvideo.core.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerManagerPlaybackSpeedTest {

    @Test
    fun setSpeedSkipsRedundantPlaybackParameterUpdates() {
        val source = String(Files.readAllBytes(playerManagerSource()))
        val setSpeed = source
            .substringAfter("fun setSpeed(speed: Float, pitch: Float = 1.0f)")
            .substringBefore("\n    fun setRepeatMode")

        assertTrue(
            "Repeated speed commits should not reconfigure ExoPlayer when speed and pitch are unchanged.",
            setSpeed.contains("playbackParameters") &&
                setSpeed.contains("current.speed == speed") &&
                setSpeed.contains("current.pitch == pitch") &&
                setSpeed.contains("return")
        )
    }

    private fun playerManagerSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "player",
            "PlayerManager.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
