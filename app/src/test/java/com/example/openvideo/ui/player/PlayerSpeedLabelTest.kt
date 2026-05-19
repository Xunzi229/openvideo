package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSpeedLabelTest {

    @Test
    fun integerSpeedsDropFractionalPart() {
        assertEquals("1x", PlayerSpeedLabel.format(1f))
        assertEquals("2x", PlayerSpeedLabel.format(2f))
    }

    @Test
    fun fractionalSpeedsKeepFraction() {
        assertEquals("1.25x", PlayerSpeedLabel.format(1.25f))
        assertEquals("1.5x", PlayerSpeedLabel.format(1.5f))
        assertEquals("0.5x", PlayerSpeedLabel.format(0.5f))
    }

    @Test
    fun unsupportedSpeedFallsBackToDefault() {
        // DefaultPlayerSettings.supportedSpeedOrDefault snaps unsupported values to 1.0x.
        assertEquals("1x", PlayerSpeedLabel.format(7.3f))
        assertEquals("1x", PlayerSpeedLabel.format(-1f))
    }
}
