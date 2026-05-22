package com.example.openvideo.core.player

import com.example.openvideo.ui.player.PlayerTimeFormatter

object PlaybackNotificationProgressPolicy {

    const val BAR_MAX = 1000
    private const val DEFAULT_BUCKET_MS = 500L
    private const val UNKNOWN_DURATION_LABEL = "--:--"

    data class BarState(
        val visible: Boolean,
        val max: Int,
        val progress: Int
    )

    data class TimeLabels(
        val elapsed: String,
        val duration: String
    )

    fun barState(positionMs: Long, durationMs: Long): BarState {
        if (durationMs <= 0L) {
            return BarState(visible = false, max = BAR_MAX, progress = 0)
        }
        val clampedPosition = positionMs.coerceIn(0L, durationMs)
        val progress = ((clampedPosition.toDouble() / durationMs) * BAR_MAX)
            .toInt()
            .coerceIn(0, BAR_MAX)
        return BarState(visible = true, max = BAR_MAX, progress = progress)
    }

    fun timeLabels(positionMs: Long, durationMs: Long): TimeLabels {
        if (durationMs <= 0L) {
            return TimeLabels(
                elapsed = PlayerTimeFormatter.format(positionMs),
                duration = UNKNOWN_DURATION_LABEL
            )
        }
        return TimeLabels(
            elapsed = PlayerTimeFormatter.format(positionMs.coerceIn(0L, durationMs)),
            duration = PlayerTimeFormatter.format(durationMs)
        )
    }

    /** Coarse position bucket so progress can refresh without rebuilding on every millisecond. */
    fun progressBucket(positionMs: Long, bucketMs: Long = DEFAULT_BUCKET_MS): Long =
        if (bucketMs <= 0L) positionMs else positionMs / bucketMs
}
