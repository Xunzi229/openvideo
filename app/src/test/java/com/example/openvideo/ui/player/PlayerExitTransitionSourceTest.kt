package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerExitTransitionSourceTest {

    @Test
    fun playerExitUsesPolicyForTransitionStrategyAndReleaseDelay() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val finishPlayer = source
            .substringAfter("private fun finishPlayer() {")
            .substringBefore("\n    private fun suppressExitTransition()")
        val suppressExit = source
            .substringAfter("private fun suppressExitTransition() {")
            .substringBefore("\n    private fun preparePlayerExitFrame()")

        assertTrue(finishPlayer.contains("PlayerExitPolicy.finishPresentation("))
        assertTrue(finishPlayer.contains("presentation.releaseDelayMs"))
        assertTrue(suppressExit.contains("PlayerExitPolicy.transitionStrategyFor("))
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
