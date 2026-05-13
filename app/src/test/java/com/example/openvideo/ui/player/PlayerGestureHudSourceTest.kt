package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerGestureHudSourceTest {

    @Test
    fun doubleTapSeekHudSurvivesTheGestureUpThatTriggeredIt() {
        val source = sourceFile("PlayerActivity.kt").readText()

        assertTrue(source.contains("keepGestureHudAfterActionUp"))
        assertTrue(source.contains("keepGestureHudAfterActionUp = true"))
        assertTrue(source.contains("if (keepGestureHudAfterActionUp)"))
    }

    @Test
    fun doubleTapForwardAndBackwardPreferencesKeepTheirConfiguredDirection() {
        val source = sourceFile("PlayerActivity.kt").readText()
        val doubleTapBlock = source.substringAfter("override fun onDoubleTap")
            .substringBefore("override fun onLongPress")

        assertTrue(doubleTapBlock.contains("DoubleTapAction.FORWARD -> handleDoubleTapSeek(PlayerSwipeSide.RIGHT)"))
        assertTrue(doubleTapBlock.contains("DoubleTapAction.BACKWARD -> handleDoubleTapSeek(PlayerSwipeSide.LEFT)"))
        assertFalse(doubleTapBlock.contains("DoubleTapAction.FORWARD,\r\n                    DoubleTapAction.BACKWARD -> handleDoubleTapSeek(e.x)"))
        assertFalse(doubleTapBlock.contains("DoubleTapAction.FORWARD,\n                    DoubleTapAction.BACKWARD -> handleDoubleTapSeek(e.x)"))
    }

    @Test
    fun accumulatedDoubleTapSeekUsesAnchorPositionInsteadOfPotentiallyStaleUiState() {
        val source = sourceFile("PlayerActivity.kt").readText()

        assertTrue(source.contains("doubleTapSeekAnchorPositionMs"))
        assertTrue(source.contains("val anchorPosition = if (result.isAccumulated)"))
        assertTrue(source.contains("PlayerTimeline.safeSeekTarget(anchorPosition, result.deltaMs, state.duration)"))
    }

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun sourceFile(fileName: String): Path {
        val relative = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            fileName
        )
        return sequenceOf(
            relative,
            Paths.get("app").resolve(relative)
        ).first(Files::exists)
    }
}
