package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerVideoColorAdjustmentPolicyTest {

    @Test
    fun normalizesPercentToFraction() {
        val adjustments = PlayerVideoColorAdjustmentPolicy.fromPercent(25, 50)
        assertEquals(0.25f, adjustments.contrast, 0.001f)
        assertEquals(0.5f, adjustments.saturation, 0.001f)
    }
}
