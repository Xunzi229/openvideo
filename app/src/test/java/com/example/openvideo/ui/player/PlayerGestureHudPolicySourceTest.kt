package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerGestureHudPolicySourceTest {

    @Test
    fun seekHudUsesPlayerTimeFormatter() {
        val source = policySource()
        val seekBlock = source.substringAfter("fun seek(")
            .substringBefore("\n    fun level(")

        assertTrue(seekBlock.contains("PlayerTimeFormatter.format("))
        assertFalse(
            "Gesture HUD must not keep a private formatTime implementation.",
            source.contains("private fun formatTime(")
        )
    }

    private fun policySource(): String {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerGestureHudPolicy.kt"
        )
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
