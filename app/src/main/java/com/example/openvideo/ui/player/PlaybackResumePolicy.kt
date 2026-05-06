package com.example.openvideo.ui.player

object PlaybackResumePolicy {
    private const val ENDING_WINDOW_MS = 10_000L

    fun restoreTarget(savedPositionMs: Long, durationMs: Long): Long? {
        if (savedPositionMs <= 0) return null
        if (durationMs <= 0) return savedPositionMs
        return savedPositionMs.takeIf { it < durationMs - ENDING_WINDOW_MS }
    }
}
