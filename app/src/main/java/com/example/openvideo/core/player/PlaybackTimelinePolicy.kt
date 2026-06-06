package com.example.openvideo.core.player

import androidx.media3.common.C

object PlaybackTimelinePolicy {

    fun hasSeekableDuration(durationMs: Long): Boolean {
        return durationMs > 0L && durationMs != Long.MAX_VALUE && durationMs != C.TIME_UNSET
    }

    fun relativeSeekTarget(currentPositionMs: Long, durationMs: Long, deltaMs: Long): Long? {
        if (!hasSeekableDuration(durationMs)) return null
        return (currentPositionMs + deltaMs).coerceIn(0L, durationMs)
    }
}
