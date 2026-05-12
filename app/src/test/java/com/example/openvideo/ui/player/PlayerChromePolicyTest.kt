package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerChromePolicyTest {

    @Test
    fun mapsControlsOpacityPercentToAlpha() {
        assertEquals(0f, PlayerChromePolicy.maxChromeAlpha(0), 0.001f)
        assertEquals(0.85f, PlayerChromePolicy.maxChromeAlpha(85), 0.001f)
        assertEquals(1f, PlayerChromePolicy.maxChromeAlpha(100), 0.001f)
    }

    @Test
    fun clampsControlsOpacityOutsideValidRange() {
        assertEquals(0f, PlayerChromePolicy.maxChromeAlpha(-20), 0.001f)
        assertEquals(1f, PlayerChromePolicy.maxChromeAlpha(140), 0.001f)
    }

    @Test
    fun mapsAutoHideSecondsToDelayMillis() {
        assertEquals(3_000L, PlayerChromePolicy.autoHideDelayMs(3))
        assertEquals(0L, PlayerChromePolicy.autoHideDelayMs(0))
    }

    @Test
    fun onlySchedulesAutoHideWhenDelayIsPositive() {
        assertFalse(PlayerChromePolicy.shouldScheduleAutoHide(0))
        assertTrue(PlayerChromePolicy.shouldScheduleAutoHide(1_500))
    }

    @Test
    fun lockedControlsUseShortHideDelay() {
        assertEquals(1_500L, PlayerChromePolicy.lockedControlsHideDelayMs())
    }
}
