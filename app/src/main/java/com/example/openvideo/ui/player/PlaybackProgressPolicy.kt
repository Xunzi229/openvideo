package com.example.openvideo.ui.player

import kotlin.math.abs

object PlaybackProgressPolicy {
    private const val SAVE_INTERVAL_MS = 5_000L

    fun shouldSaveProgress(positionMs: Long, lastSavedPositionMs: Long): Boolean {
        if (positionMs <= 0L) return false
        return abs(positionMs - lastSavedPositionMs) >= SAVE_INTERVAL_MS
    }

    fun onPositionTick(
        positionMs: Long,
        lastSavedPositionMs: Long
    ): PlaybackProgressSaveDecision {
        val shouldSave = shouldSaveProgress(positionMs, lastSavedPositionMs)
        return PlaybackProgressSaveDecision(
            shouldSaveHistory = shouldSave,
            nextLastSavedPositionMs = if (shouldSave) positionMs else lastSavedPositionMs
        )
    }

    fun onNewMedia(): Long = 0L
}

data class PlaybackProgressSaveDecision(
    val shouldSaveHistory: Boolean,
    val nextLastSavedPositionMs: Long
)
