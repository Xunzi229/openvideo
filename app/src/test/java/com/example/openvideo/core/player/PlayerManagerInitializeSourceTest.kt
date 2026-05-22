package com.example.openvideo.core.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerManagerInitializeSourceTest {

    @Test
    fun initializeReleasesExistingPlayerBeforeCreatingAnother() {
        val source = String(Files.readAllBytes(playerManagerSource()))
        val method = source.substringAfter("fun initialize(mediaUri: Uri? = null): ExoPlayer {")
            .substringBefore("\n    fun release()")

        assertTrue(
            "initialize must release the previous ExoPlayer; PlayerManager is a singleton shared by PlayerActivity and PlaybackService.",
            method.contains("release()")
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
