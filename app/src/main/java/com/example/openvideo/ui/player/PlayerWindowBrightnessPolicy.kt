package com.example.openvideo.ui.player

object PlayerWindowBrightnessPolicy {
    fun initialBrightness(windowBrightness: Float, systemBrightnessNormalized: Float?): Float =
        if (windowBrightness in 0f..1f) {
            windowBrightness
        } else {
            systemBrightnessNormalized?.coerceIn(0f, 1f) ?: 0.5f
        }

    fun initialVolumeLevel(currentVolume: Int, maxVolume: Int): Float =
        if (maxVolume > 0) {
            currentVolume.toFloat() / maxVolume
        } else {
            0.5f
        }

    fun levelToProgressPercent(level: Float): Int =
        (level.coerceIn(0f, 1f) * 100f).toInt()
}
