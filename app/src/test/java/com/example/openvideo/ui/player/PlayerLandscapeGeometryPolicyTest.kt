package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerLandscapeGeometryPolicyTest {

    @Test
    fun returnsNullForUnmeasuredContainer() {
        assertNull(PlayerLandscapeGeometryPolicy.compute(0, 1080, 3f))
        assertNull(PlayerLandscapeGeometryPolicy.compute(1920, 0, 3f))
        assertNull(PlayerLandscapeGeometryPolicy.compute(-10, 1080, 3f))
    }

    @Test
    fun marginsAreRawRatiosOfContainerSize() {
        val g = PlayerLandscapeGeometryPolicy.compute(widthPx = 1920, heightPx = 1080, density = 3f)
        assertNotNull(g)
        g!!
        assertEquals((1920 * 0.022f).toInt(), g.containerHorizontalMarginPx)
        assertEquals((1080 * 0.028f).toInt(), g.topBarTopMarginPx)
        assertEquals((1080 * 0.032f).toInt(), g.bottomPanelBottomMarginPx)
        assertEquals((1920 * 0.026f).toInt(), g.lockMarginStartPx)
    }

    @Test
    fun mediumWidthAtDensityOneKeepsTransportRawRatios() {
        // density = 1 keeps dp clamp ranges low (40..52, 52..64, 14..22, 6..12) so width*ratio wins.
        val g = PlayerLandscapeGeometryPolicy.compute(widthPx = 900, heightPx = 480, density = 1f)!!
        assertEquals((900 * 0.049f).toInt(), g.iconSidePx)      // 44 (in [40, 52])
        assertEquals((900 * 0.060f).toInt(), g.playSidePx)      // 54 (in [52, 64])
        assertEquals((900 * 0.020f).toInt(), g.transportGapPx)  // 18 (in [14, 22])
        assertEquals((900 * 0.009f).toInt(), g.innerGapPx)      // 8  (in [6, 12])
    }

    @Test
    fun smallScreenClampsTransportToMinimumDp() {
        // Phone-class width with density = 3 makes every raw ratio fall below MIN_DP * density.
        val g = PlayerLandscapeGeometryPolicy.compute(widthPx = 1920, heightPx = 1080, density = 3f)!!
        assertEquals((40f * 3f).toInt(), g.iconSidePx)
        assertEquals((52f * 3f).toInt(), g.playSidePx)
        assertEquals((14f * 3f).toInt(), g.transportGapPx)
        assertEquals((6f * 3f).toInt(), g.innerGapPx)
    }

    @Test
    fun hugeWidthAtDensityOneClampsTransportToMaximumDp() {
        // density = 1 + 4K width pushes raw ratios beyond MAX_DP, all sizes pin to the upper bound.
        val g = PlayerLandscapeGeometryPolicy.compute(widthPx = 3840, heightPx = 2160, density = 1f)!!
        assertEquals(52, g.iconSidePx)
        assertEquals(64, g.playSidePx)
        assertEquals(22, g.transportGapPx)
        assertEquals(12, g.innerGapPx)
    }

    @Test
    fun nonPositiveDensityFallsBackToOne() {
        // A misreported density (0 or negative) must not collapse dp clamps to zero; treat it as 1x.
        val zero = PlayerLandscapeGeometryPolicy.compute(widthPx = 3840, heightPx = 2160, density = 0f)!!
        val negative = PlayerLandscapeGeometryPolicy.compute(widthPx = 3840, heightPx = 2160, density = -2f)!!
        val one = PlayerLandscapeGeometryPolicy.compute(widthPx = 3840, heightPx = 2160, density = 1f)!!
        assertEquals(one.iconSidePx, zero.iconSidePx)
        assertEquals(one.playSidePx, zero.playSidePx)
        assertEquals(one.transportGapPx, zero.transportGapPx)
        assertEquals(one.innerGapPx, zero.innerGapPx)
        assertEquals(one.iconSidePx, negative.iconSidePx)
    }

    @Test
    fun marginRatiosAreIndependentOfDensity() {
        // Margins (container / top / bottom / lock) are pure ratios, density never affects them.
        val a = PlayerLandscapeGeometryPolicy.compute(widthPx = 2400, heightPx = 1080, density = 2f)!!
        val b = PlayerLandscapeGeometryPolicy.compute(widthPx = 2400, heightPx = 1080, density = 4f)!!
        assertEquals(a.containerHorizontalMarginPx, b.containerHorizontalMarginPx)
        assertEquals(a.topBarTopMarginPx, b.topBarTopMarginPx)
        assertEquals(a.bottomPanelBottomMarginPx, b.bottomPanelBottomMarginPx)
        assertEquals(a.lockMarginStartPx, b.lockMarginStartPx)
    }
}
