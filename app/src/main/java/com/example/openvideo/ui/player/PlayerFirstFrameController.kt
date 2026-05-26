package com.example.openvideo.ui.player

import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.openvideo.core.diagnostics.CrashLogger

class PlayerFirstFrameController(
    private val activity: AppCompatActivity,
    private val handler: Handler,
    private val startupTrace: PlayerStartupTrace,
    private val firstFrameScrimProvider: () -> View?,
    private val hasVideoTrackProvider: () -> Boolean
) {
    var isAwaitingFirstFrame = true
        private set
    private var hasLoggedFirstFrame = false
    private var hasLoggedFirstFrameTimeout = false
    private var isFirstFrameTimeoutPosted = false

    private val firstFrameTimeoutRunnable = Runnable {
        isFirstFrameTimeoutPosted = false
        if (hasLoggedFirstFrame || hasLoggedFirstFrameTimeout) return@Runnable
        hasLoggedFirstFrameTimeout = true
        startupTrace.recordOnce(PlayerStartupTrace.Events.FIRST_FRAME_TIMEOUT)
        CrashLogger.logDiagnostic(activity, "player_first_frame_timeout", startupTrace.format())
    }

    fun resetForNewVideo(awaitFirstFrame: Boolean) {
        isAwaitingFirstFrame = awaitFirstFrame
    }

    fun showForNewMedia() {
        applyDecision(PlayerFirstFramePolicy.onShowForNewMedia())
    }

    fun onRenderedFirstFrame() {
        hideOnRenderedFirstFrame()
        cancelTimeoutCheck()
        if (!hasLoggedFirstFrame) {
            hasLoggedFirstFrame = true
            startupTrace.record(PlayerStartupTrace.Events.FIRST_FRAME_RENDERED)
            CrashLogger.logDiagnostic(
                activity,
                "player_startup",
                startupTrace.format()
            )
        }
    }

    fun hideOnRenderedFirstFrame() {
        applyDecision(
            PlayerFirstFramePolicy.onRenderedFirstFrame(
                isAwaitingFirstFrame = isAwaitingFirstFrame
            )
        )
    }

    fun hideForAudioOnly() {
        applyDecision(
            PlayerFirstFramePolicy.onReady(
                isAwaitingFirstFrame = isAwaitingFirstFrame,
                hasVideoTrack = hasVideoTrackProvider()
            )
        )
    }

    fun onPrepareReady() {
        if (startupTrace.hasRecorded(PlayerStartupTrace.Events.PREPARE_READY)) return
        startupTrace.recordOnce(PlayerStartupTrace.Events.PREPARE_READY)
        scheduleTimeoutCheck()
    }

    fun cancelTimeoutCheck() {
        if (!isFirstFrameTimeoutPosted) return
        handler.removeCallbacks(firstFrameTimeoutRunnable)
        isFirstFrameTimeoutPosted = false
    }

    private fun scheduleTimeoutCheck() {
        val delayMs = PlayerFirstFrameTimeoutPolicy.scheduleDelayMs(
            hasVideoTrack = hasVideoTrackProvider(),
            firstFrameRendered = hasLoggedFirstFrame,
            alreadyTimedOut = hasLoggedFirstFrameTimeout
        ) ?: return
        cancelTimeoutCheck()
        handler.postDelayed(firstFrameTimeoutRunnable, delayMs)
        isFirstFrameTimeoutPosted = true
    }

    private fun applyDecision(decision: PlayerFirstFrameDecision) {
        isAwaitingFirstFrame = decision.nextAwaitingFirstFrame
        val firstFrameScrim = firstFrameScrimProvider() ?: return

        val presentation = PlayerFirstFrameScrimPolicy.presentation(decision) ?: return
        firstFrameScrim.animate().cancel()
        firstFrameScrim.alpha = presentation.alpha
        firstFrameScrim.visibility = if (presentation.visible) View.VISIBLE else View.GONE
    }
}
