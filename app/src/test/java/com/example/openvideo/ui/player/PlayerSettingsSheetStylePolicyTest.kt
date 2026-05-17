package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSettingsSheetStylePolicyTest {

    @Test
    fun computesPanelDimAndBlurRadius() {
        val style = PlayerSettingsSheetStylePolicy.compute(
            panelOpacityPercent = 85,
            backdropDimPercent = 40,
            backdropBlurDp = 8,
            density = 2f
        )
        assertEquals(0.85f, style.panelAlpha, 0.001f)
        assertEquals(0.4f, style.dimAmount, 0.001f)
        assertEquals(16, style.backdropBlurRadiusPx)
    }

    @Test
    fun clampsBlurDpAndSkipsRadiusWhenZero() {
        val style = PlayerSettingsSheetStylePolicy.compute(
            panelOpacityPercent = 100,
            backdropDimPercent = 0,
            backdropBlurDp = 99,
            density = 3f
        )
        assertEquals(192, style.backdropBlurRadiusPx)
    }
}
