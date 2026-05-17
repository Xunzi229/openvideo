package com.example.openvideo.ui.player

object PlayerChromePolicy {
    private const val LOCKED_CONTROLS_HIDE_DELAY_MS = 1_500L
    const val CHROME_FADE_DURATION_MS = 200L

    fun percentToAlpha(percent: Int): Float =
        percent.coerceIn(0, 100) / 100f

    fun maxChromeAlpha(controlsOpacityPercent: Int): Float =
        percentToAlpha(controlsOpacityPercent)

    fun autoHideDelayMs(autoHideSeconds: Int): Long =
        autoHideSeconds.coerceAtLeast(0) * 1_000L

    fun shouldScheduleAutoHide(delayMs: Long): Boolean =
        delayMs > 0L

    fun lockedControlsHideDelayMs(): Long = LOCKED_CONTROLS_HIDE_DELAY_MS
}
