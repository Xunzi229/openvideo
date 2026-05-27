package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerExitTransitionSourceTest {

    @Test
    fun playerExitUsesPolicyForTransitionStrategyAndReleaseDelay() {
        val source = String(Files.readAllBytes(playerExitControllerSource()))
        val finishPlayer = source
            .substringAfter("fun finishPlayer() {")
            .substringBefore("\n    private fun suppressExitTransition()")
        val suppressExit = source
            .substringAfter("private fun suppressExitTransition() {")
            .substringBefore("\n    @RequiresApi")

        assertTrue(finishPlayer.contains("PlayerExitPolicy.finishPresentation("))
        assertTrue(finishPlayer.contains("presentation.releaseDelayMs"))
        assertTrue(suppressExit.contains("PlayerExitPolicy.transitionStrategyFor("))
    }

    private fun playerActivitySource(): Path {
        return kotlinSource("PlayerActivity.kt")
    }

    private fun playerExitControllerSource(): Path {
        return kotlinSource("PlayerExitController.kt")
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
