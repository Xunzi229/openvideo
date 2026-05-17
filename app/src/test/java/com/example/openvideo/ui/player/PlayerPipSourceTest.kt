package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerPipSourceTest {

    @Test
    fun playerActivityDelegatesPipEntryDecisionToPolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val pipBlock = source.substringAfter("private fun enterPipModeIfSupported() {")
            .substringBefore("\n    private fun startPlaybackServiceIfNeeded")

        assertTrue(pipBlock.contains("PlayerPipPolicy.enterDecision("))
        assertTrue(pipBlock.contains("PictureInPictureParams.Builder()"))
        assertTrue(pipBlock.contains(".setAspectRatio("))
        assertTrue(pipBlock.contains("decision.aspectRatio?.toRational()"))
        assertTrue(pipBlock.contains("PlayerPipPolicy.fallbackRational()"))
        assertFalse(
            "PiP fallback must not inline Rational(16, 9) in Activity.",
            pipBlock.contains("Rational(16, 9)")
        )
        assertFalse(
            "Activity must not declare a private PlayerPipAspectRatio.toRational extension.",
            source.contains("private fun PlayerPipAspectRatio.toRational()")
        )
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
