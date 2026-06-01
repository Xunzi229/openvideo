package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerFirstFrameTimeoutSourceTest {

    @Test
    fun playerActivityRoutesPrepareReadyThroughStartupTraceConstant() {
        val source = playerActivitySource()
        val controller = playerFirstFrameControllerSource()
        assertTrue(
            "STATE_READY branch must call onPrepareReady().",
            source.contains("firstFrames.hideForAudioOnly()") &&
                source.contains("firstFrames.onPrepareReady()")
        )
        assertTrue(
            "onPrepareReady must record the PREPARE_READY constant once.",
            controller.contains("startupTrace.recordOnce(PlayerStartupTrace.Events.PREPARE_READY)")
        )
        assertTrue(
            "onPrepareReady must schedule a first-frame timeout check.",
            controller.contains("scheduleTimeoutCheck()")
        )
    }

    @Test
    fun playerActivityUsesTimeoutPolicyToDecideDelay() {
        val source = playerFirstFrameControllerSource()
        assertTrue(
            "scheduleFirstFrameTimeoutCheck must delegate the delay decision to " +
                "PlayerFirstFrameTimeoutPolicy.",
            source.contains("PlayerFirstFrameTimeoutPolicy.scheduleDelayMs(")
        )
        assertTrue(
            "Timeout policy must read the latest first-frame state from the activity flags.",
            source.contains("firstFrameRendered = hasLoggedFirstFrame") &&
                source.contains("alreadyTimedOut = hasLoggedFirstFrameTimeout")
        )
    }

    @Test
    fun firstFrameTimeoutRunnableEmitsDiagnosticOnceAndCancelsOnFirstFrame() {
        val source = playerFirstFrameControllerSource()
        assertTrue(
            "Runnable must guard with already-rendered/already-timed-out flags.",
            source.contains("if (hasLoggedFirstFrame || hasLoggedFirstFrameTimeout) return@Runnable")
        )
        assertTrue(
            "Runnable must record the FIRST_FRAME_TIMEOUT event.",
            source.contains("startupTrace.recordOnce(PlayerStartupTrace.Events.FIRST_FRAME_TIMEOUT)")
        )
        assertTrue(
            "Runnable must write the diagnostic log named player_first_frame_timeout.",
            source.contains("CrashLogger.logDiagnostic(activity, \"player_first_frame_timeout\"")
        )
        assertTrue(
            "onRenderedFirstFrame must cancel the pending timeout check.",
            source.contains("cancelTimeoutCheck()")
        )
    }

    private fun playerActivitySource(): String {
        return kotlinSource("PlayerActivity.kt")
    }

    private fun playerFirstFrameControllerSource(): String {
        return kotlinSource("PlayerFirstFrameController.kt")
    }

    private fun kotlinSource(name: String): String {
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
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
