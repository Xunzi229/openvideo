package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerLockPauseSourceTest {

    @Test
    fun pausingOnExitClearsPlayerLockBeforePausing() {
        val source = String(Files.readAllBytes(playerLifecycleControllerSource()))
        val onPause = source.substringAfter("fun onPause() {")
            .substringBefore("\n    fun", missingDelimiterValue = source.substringAfter("fun onPause() {"))

        assertTrue(onPause.contains("PlayerLifecyclePolicy.onPause("))
        val unlockIndex = onPause.indexOf("onUnlockPlayerForPause()")
        val pauseIndex = onPause.indexOf("pause()")

        assertTrue("onPause should unlock the player when it is about to pause playback", unlockIndex >= 0)
        assertTrue(
            "onPause should unlock before pausing so returning to the player is not trapped behind lock mode",
            unlockIndex < pauseIndex
        )
    }

    @Test
    fun playerPauseEventsClearPlayerLock() {
        val source = String(Files.readAllBytes(playerEventControllerSource()))

        assertTrue(
            "Player listener should clear lock when playback changes to a paused intent",
            source.contains("if (!playWhenReady) unlockPlayerForPause()")
                || source.contains("if (!playWhenReady) onUnlockPlayerForPause()")
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
        val source = String(Files.readAllBytes(playerLifecycleControllerSource()))
        val onResume = source.substringAfter("fun onResume() {")
            .substringBefore("\n    fun onPause()")
        val onPause = source.substringAfter("fun onPause() {")
            .substringBefore("\n    fun", missingDelimiterValue = source.substringAfter("fun onPause() {"))

        assertTrue(onResume.contains("PlayerLifecyclePolicy.onResume()"))
        assertTrue(onResume.contains("if (decision.stopPlaybackService) onStopPlaybackService()"))
        assertTrue(onResume.contains("if (decision.observeState) onObserveState()"))
        assertTrue(onResume.contains("onApplyDisplaySettings()"))

        assertTrue(onPause.contains("PlayerLifecyclePolicy.onPause("))
        assertTrue(onPause.contains("if (decision.saveHistory) viewModel.saveHistory()"))
        assertTrue(onPause.contains("if (decision.pausePlayer)"))
        assertTrue(onPause.contains("if (decision.startPlaybackService)"))
    }

    @Test
    fun backgroundPlaybackServiceStartUsesPurePolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val controller = String(Files.readAllBytes(playerNotificationControllerSource()))

        assertTrue(source.contains("playbackNotifications.startIfNeeded(isPlaying)"))
        assertTrue(controller.contains("PlayerBackgroundServicePolicy.startDecision("))
        assertTrue(controller.contains("if (!decision.shouldStart) return"))
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

    private fun playerNotificationControllerSource(): Path {
        return playerSource("PlayerPlaybackNotificationController.kt")
    }

    private fun playerEventControllerSource(): Path {
        return playerSource("PlayerEventController.kt")
    }

    private fun playerLifecycleControllerSource(): Path {
        return playerSource("PlayerLifecycleController.kt")
    }

    private fun playerSource(name: String): Path {
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
