package com.example.openvideo.ui.player

import androidx.media3.common.Player

/**
 * Maps ExoPlayer playback-state transitions to startup-trace event names.
 * Separates the first [PlayerStartupTrace.Events.PREPARE_READY] from later
 * [PlayerStartupTrace.Events.READY_AFTER_BUFFERING] recoveries.
 */
object PlayerPlaybackReadyTracePolicy {

    enum class ReadyTraceEvent {
        FIRST_PREPARE_READY,
        RECOVERED_AFTER_BUFFERING
    }

    data class Update(
        val readyTraceEvent: ReadyTraceEvent?,
        val nextWasBuffering: Boolean
    )

    fun onPlaybackStateChanged(
        playbackState: Int,
        wasBuffering: Boolean,
        hasRecordedPrepareReady: Boolean
    ): Update =
        when (playbackState) {
            Player.STATE_BUFFERING -> Update(
                readyTraceEvent = null,
                nextWasBuffering = true
            )
            Player.STATE_READY -> Update(
                readyTraceEvent = when {
                    !hasRecordedPrepareReady -> ReadyTraceEvent.FIRST_PREPARE_READY
                    wasBuffering -> ReadyTraceEvent.RECOVERED_AFTER_BUFFERING
                    else -> null
                },
                nextWasBuffering = false
            )
            else -> Update(
                readyTraceEvent = null,
                nextWasBuffering = wasBuffering
            )
        }
}
