package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerLockPauseSourceTest {

    @Test
    fun pausingOnExitClearsPlayerLockBeforePausing() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val onPause = source.substringAfter("override fun onPause() {")
            .substringBefore("\n    override fun onDestroy()")

        assertTrue(onPause.contains("PlayerLifecyclePolicy.onPause("))
        val unlockIndex = onPause.indexOf("unlockPlayerForPause()")
        val pauseIndex = onPause.indexOf("pause()")

        assertTrue("onPause should unlock the player when it is about to pause playback", unlockIndex >= 0)
        assertTrue(
            "onPause should unlock before pausing so returning to the player is not trapped behind lock mode",
            unlockIndex < pauseIndex
        )
    }

    @Test
    fun playerPauseEventsClearPlayerLock() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(
            "Player listener should clear lock when playback changes to a paused intent",
            source.contains("if (!playWhenReady) unlockPlayerForPause()")
        )
    }

    @Test
    fun backPressUsesLockPolicyBeforeFinishingPlayer() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val backCallback = source.substringAfter("override fun handleOnBackPressed() {")
            .substringBefore("\n            }")

        assertTrue(backCallback.contains("PlayerLockGesturePolicy.onBackPressed(isScreenLocked)"))
        assertTrue(backCallback.contains("if (decision.revealLockedControls) showLockedControls()"))
        assertTrue(backCallback.contains("if (decision.finishPlayer) finishPlayer()"))
    }

    @Test
    fun resumeAndPauseLifecycleUsePurePolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val onResume = source.substringAfter("override fun onResume() {")
            .substringBefore("\n    override fun onPause()")
        val onPause = source.substringAfter("override fun onPause() {")
            .substringBefore("\n    override fun onDestroy()")

        assertTrue(onResume.contains("PlayerLifecyclePolicy.onResume()"))
        assertTrue(onResume.contains("if (decision.stopPlaybackService) stopPlaybackService()"))
        assertTrue(onResume.contains("if (decision.observeState) observeState()"))

        assertTrue(onPause.contains("PlayerLifecyclePolicy.onPause("))
        assertTrue(onPause.contains("if (decision.saveHistory) viewModel.saveHistory()"))
        assertTrue(onPause.contains("if (decision.pausePlayer)"))
        assertTrue(onPause.contains("if (decision.startPlaybackService)"))
    }

    @Test
    fun backgroundPlaybackServiceStartUsesPurePolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val serviceStart = source.substringAfter("private fun startPlaybackServiceIfNeeded")
            .substringBefore("\n    private fun stopPlaybackService")

        assertTrue(serviceStart.contains("PlayerBackgroundServicePolicy.startDecision("))
        assertTrue(serviceStart.contains("if (!decision.shouldStart) return"))
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
