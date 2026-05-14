package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLevelAdjustmentPolicyTest {

    @Test
    fun verticalBrightnessDragUsesOnePercentMinimum() {
        val adjustment = PlayerLevelAdjustmentPolicy.verticalBrightness(
            anchor = 0.5f,
            dy = 1000f,
            screenHeightPx = 1000
        )

        assertEquals(0.01f, adjustment.level, 0.001f)
        assertEquals(1, adjustment.progressPercent)
    }

    @Test
    fun horizontalBrightnessDragClampsAtMaximum() {
        val adjustment = PlayerLevelAdjustmentPolicy.horizontalBrightness(
            anchor = 0.8f,
            dx = 500f,
            screenWidthPx = 1000
        )

        assertEquals(1f, adjustment.level, 0.001f)
        assertEquals(100, adjustment.progressPercent)
    }

    @Test
    fun verticalVolumeDragCanReachZero() {
        val adjustment = PlayerLevelAdjustmentPolicy.verticalVolume(
            anchor = 0.2f,
            dy = 500f,
            screenHeightPx = 1000,
            maxVolume = 15
        )

        assertEquals(0f, adjustment.level, 0.001f)
        assertEquals(0, adjustment.progressPercent)
        assertEquals(0, adjustment.streamVolume)
    }

    @Test
    fun horizontalVolumeDragMapsToStreamVolume() {
        val adjustment = PlayerLevelAdjustmentPolicy.horizontalVolume(
            anchor = 0.4f,
            dx = 200f,
            screenWidthPx = 1000,
            maxVolume = 10
        )

        assertEquals(0.6f, adjustment.level, 0.001f)
        assertEquals(60, adjustment.progressPercent)
        assertEquals(6, adjustment.streamVolume)
    }

    @Test
    fun verticalBrightnessCanReachMaximumWithinHalfScreenTravel() {
        val adjustment = PlayerLevelAdjustmentPolicy.verticalBrightness(
            anchor = 0.01f,
            dy = -500f,
            screenHeightPx = 1000
        )

        assertEquals(1f, adjustment.level, 0.001f)
        assertEquals(100, adjustment.progressPercent)
    }

    @Test
    fun verticalVolumeCanReachMaximumWithinHalfScreenTravel() {
        val adjustment = PlayerLevelAdjustmentPolicy.verticalVolume(
            anchor = 0f,
            dy = -500f,
            screenHeightPx = 1000,
            maxVolume = 15
        )

        assertEquals(1f, adjustment.level, 0.001f)
        assertEquals(100, adjustment.progressPercent)
        assertEquals(15, adjustment.streamVolume)
    }

    @Test
    fun invalidScreenSizeKeepsClampedAnchor() {
        val adjustment = PlayerLevelAdjustmentPolicy.verticalBrightness(
            anchor = 2f,
            dy = -500f,
            screenHeightPx = 0
        )

        assertEquals(1f, adjustment.level, 0.001f)
        assertEquals(100, adjustment.progressPercent)
    }
}
