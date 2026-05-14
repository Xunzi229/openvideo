package com.example.openvideo.ui.player

data class PlayerSeekGesturePreview(
    val deltaMs: Long,
    val targetMs: Long,
    val seekable: Boolean,
    val isClamped: Boolean,
    val timelineFraction: Float?
)

object PlayerSeekGesturePolicy {
    private const val LOW_SENSITIVITY_WINDOW_MS = 30_000L
    private const val MEDIUM_SENSITIVITY_WINDOW_MS = 60_000L
    private const val HIGH_SENSITIVITY_WINDOW_MS = 90_000L

    fun horizontalDeltaMs(dx: Float, screenWidthPx: Int, sensitivity: Int): Long {
        if (screenWidthPx <= 0) return 0L
        return (dx / screenWidthPx * seekWindowMs(sensitivity)).toLong()
    }

    fun horizontalPreview(
        anchorPositionMs: Long,
        dx: Float,
        screenWidthPx: Int,
        durationMs: Long,
        sensitivity: Int
    ): PlayerSeekGesturePreview {
        val deltaMs = horizontalDeltaMs(dx, screenWidthPx, sensitivity)
        val targetMs = PlayerTimeline.safeSeekTarget(anchorPositionMs, deltaMs, durationMs)
        val rawTargetMs = anchorPositionMs + deltaMs
        val seekable = PlayerTimeline.hasSeekableDuration(durationMs)
        return PlayerSeekGesturePreview(
            deltaMs = deltaMs,
            targetMs = targetMs,
            seekable = seekable,
            isClamped = targetMs != rawTargetMs,
            timelineFraction = if (seekable) {
                (targetMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            }
        )
    }

    fun verticalDeltaMs(dy: Float, screenHeightPx: Int, sensitivity: Int): Long {
        if (screenHeightPx <= 0) return 0L
        return (-dy / screenHeightPx * seekWindowMs(sensitivity)).toLong()
    }

    fun verticalPreview(
        anchorPositionMs: Long,
        dy: Float,
        screenHeightPx: Int,
        durationMs: Long,
        sensitivity: Int
    ): PlayerSeekGesturePreview {
        val deltaMs = verticalDeltaMs(dy, screenHeightPx, sensitivity)
        val targetMs = PlayerTimeline.safeSeekTarget(anchorPositionMs, deltaMs, durationMs)
        val rawTargetMs = anchorPositionMs + deltaMs
        val seekable = PlayerTimeline.hasSeekableDuration(durationMs)
        return PlayerSeekGesturePreview(
            deltaMs = deltaMs,
            targetMs = targetMs,
            seekable = seekable,
            isClamped = targetMs != rawTargetMs,
            timelineFraction = if (seekable) {
                (targetMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            }
        )
    }

    private fun seekWindowMs(sensitivity: Int): Long = when (sensitivity) {
        1 -> LOW_SENSITIVITY_WINDOW_MS
        2 -> MEDIUM_SENSITIVITY_WINDOW_MS
        else -> HIGH_SENSITIVITY_WINDOW_MS
    }
}
