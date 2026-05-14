package com.example.openvideo.ui.player

data class PlayerLevelAdjustment(
    val level: Float,
    val progressPercent: Int = 0,
    val streamVolume: Int? = null
)

object PlayerLevelAdjustmentPolicy {
    private const val VERTICAL_REACHABLE_TRAVEL_RATIO = 0.5f

    fun verticalBrightness(
        anchor: Float,
        dy: Float,
        screenHeightPx: Int
    ): PlayerLevelAdjustment =
        PlayerLevelAdjustment(
            level = verticalReachableLevel(
                anchor = anchor,
                dy = dy,
                screenHeightPx = screenHeightPx,
                min = 0.01f
            )
        ).withProgress()

    fun horizontalBrightness(
        anchor: Float,
        dx: Float,
        screenWidthPx: Int
    ): PlayerLevelAdjustment =
        PlayerLevelAdjustment(
            level = PlayerGesturePolicy.horizontalLevel(
                anchor = anchor,
                dx = dx,
                screenWidthPx = screenWidthPx,
                min = 0.01f
            )
        ).withProgress()

    fun verticalVolume(
        anchor: Float,
        dy: Float,
        screenHeightPx: Int,
        maxVolume: Int
    ): PlayerLevelAdjustment {
        val level = verticalReachableLevel(
            anchor = anchor,
            dy = dy,
            screenHeightPx = screenHeightPx
        )
        return volumeAdjustment(level, maxVolume)
    }

    fun horizontalVolume(
        anchor: Float,
        dx: Float,
        screenWidthPx: Int,
        maxVolume: Int
    ): PlayerLevelAdjustment {
        val level = PlayerGesturePolicy.horizontalLevel(
            anchor = anchor,
            dx = dx,
            screenWidthPx = screenWidthPx
        )
        return volumeAdjustment(level, maxVolume)
    }

    private fun volumeAdjustment(level: Float, maxVolume: Int): PlayerLevelAdjustment =
        PlayerLevelAdjustment(
            level = level,
            streamVolume = (level * maxVolume).toInt().coerceIn(0, maxVolume.coerceAtLeast(0))
        ).withProgress()

    private fun verticalReachableLevel(
        anchor: Float,
        dy: Float,
        screenHeightPx: Int,
        min: Float = 0f,
        max: Float = 1f
    ): Float {
        if (screenHeightPx <= 0) return anchor.coerceIn(min, max)
        val reachableTravelPx = screenHeightPx * VERTICAL_REACHABLE_TRAVEL_RATIO
        return (anchor - dy / reachableTravelPx).coerceIn(min, max)
    }

    private fun PlayerLevelAdjustment.withProgress(): PlayerLevelAdjustment =
        copy(progressPercent = (level * 100).toInt().coerceIn(0, 100))
}
