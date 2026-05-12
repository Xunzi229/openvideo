package com.example.openvideo.ui.player

import kotlin.math.abs

enum class PlayerSwipeSide { LEFT, RIGHT, NONE }
enum class PlayerSwipeAxis { HORIZONTAL, VERTICAL, NONE }

object PlayerGesturePolicy {
    private const val SEEK_WINDOW_MS = 60_000L

    fun gestureSlopPx(sensitivity: Int): Int = when (sensitivity) {
        1 -> 60
        2 -> 50
        else -> 40
    }

    fun edgeThresholdPx(screenWidthPx: Int): Float =
        screenWidthPx.coerceAtLeast(0) * 0.05f

    fun isEdgeSwipe(x: Float, screenWidthPx: Int): Boolean {
        if (screenWidthPx <= 0) return false
        val threshold = edgeThresholdPx(screenWidthPx)
        return x < threshold || x > screenWidthPx - threshold
    }

    fun swipeSide(x: Float, screenWidthPx: Int): PlayerSwipeSide {
        if (screenWidthPx <= 0) return PlayerSwipeSide.NONE
        return if (x < screenWidthPx / 2f) PlayerSwipeSide.LEFT else PlayerSwipeSide.RIGHT
    }

    fun dominantAxis(dx: Float, dy: Float, slopPx: Int): PlayerSwipeAxis {
        if (abs(dx) <= slopPx && abs(dy) <= slopPx) return PlayerSwipeAxis.NONE
        return if (abs(dx) > abs(dy)) PlayerSwipeAxis.HORIZONTAL else PlayerSwipeAxis.VERTICAL
    }

    fun horizontalSeekDeltaMs(dx: Float, screenWidthPx: Int): Long {
        if (screenWidthPx <= 0) return 0L
        return (dx / screenWidthPx * SEEK_WINDOW_MS).toLong()
    }

    fun verticalSeekDeltaMs(dy: Float, screenHeightPx: Int): Long {
        if (screenHeightPx <= 0) return 0L
        return (-dy / screenHeightPx * SEEK_WINDOW_MS).toLong()
    }

    fun verticalLevel(
        anchor: Float,
        dy: Float,
        screenHeightPx: Int,
        min: Float = 0f,
        max: Float = 1f
    ): Float {
        if (screenHeightPx <= 0) return anchor.coerceIn(min, max)
        return (anchor - dy / screenHeightPx).coerceIn(min, max)
    }

    fun horizontalLevel(
        anchor: Float,
        dx: Float,
        screenWidthPx: Int,
        min: Float = 0f,
        max: Float = 1f
    ): Float {
        if (screenWidthPx <= 0) return anchor.coerceIn(min, max)
        return (anchor + dx / screenWidthPx).coerceIn(min, max)
    }
}
