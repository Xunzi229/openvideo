package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerWindowBrightnessPolicyTest {

    @Test
    fun prefersWindowBrightnessWhenInRange() {
        assertEquals(0.75f, PlayerWindowBrightnessPolicy.initialBrightness(0.75f, 0.2f))
    }

    @Test
    fun fallsBackToSystemBrightnessWhenWindowUnset() {
        assertEquals(0.4f, PlayerWindowBrightnessPolicy.initialBrightness(-1f, 0.4f))
    }

    @Test
    fun defaultsWhenWindowAndSystemMissing() {
        assertEquals(0.5f, PlayerWindowBrightnessPolicy.initialBrightness(-1f, null))
    }

    @Test
    fun normalizesVolumeLevel() {
        assertEquals(0.5f, PlayerWindowBrightnessPolicy.initialVolumeLevel(5, 10))
        assertEquals(0.5f, PlayerWindowBrightnessPolicy.initialVolumeLevel(0, 0))
    }

    @Test
    fun levelToProgressPercentClamps() {
        assertEquals(75, PlayerWindowBrightnessPolicy.levelToProgressPercent(0.75f))
        assertEquals(100, PlayerWindowBrightnessPolicy.levelToProgressPercent(1.5f))
    }
}
