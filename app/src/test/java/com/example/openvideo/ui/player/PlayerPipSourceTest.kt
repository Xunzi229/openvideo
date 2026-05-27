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
        val activitySource = String(Files.readAllBytes(playerActivitySource()))
        val source = String(Files.readAllBytes(playerPipControllerSource()))
        val pipBlock = source.substringAfter("fun enterIfSupported() {")
            .substringBefore("\n    fun onPictureInPictureModeChanged")

        assertTrue(activitySource.contains("private fun enterPipModeIfSupported() = playerPip.enterIfSupported()"))
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
        return kotlinSource("PlayerActivity.kt")
    }

    private fun playerPipControllerSource(): Path {
        return kotlinSource("PlayerPipController.kt")
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
